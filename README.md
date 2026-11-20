# Territory Intelligence Dashboard

Painel de inteligência territorial para antecipação de problemas e apoio à decisão humana.
**GS2026.2 — Global Solution · Cross-Platform Application Development · FIAP**

| | |
|---|---|
| **Plataforma** | Android nativo — Kotlin + Jetpack Compose |
| **minSdk / targetSdk** | 24 (Android 7.0) / 36 |
| **Arquitetura** | MVVM com fluxo unidirecional sobre blueprint em 3 camadas |
| **Persistência** | Room 2.8.5 (SQLite) + SharedPreferences |
| **Rede** | Nenhuma. O app **não declara** a permissão de INTERNET |

### Integrantes

| RM | Nome | Camada sob responsabilidade |
|---|---|---|
| 563651 | Henrique Nogueira Boscoli | Domínio (motor de previsão, casos de uso, testes) |
| 565566 | Tomazzo Canterucci | Dados e persistência (Room, seed, repositório) |
| 563578 | João Henrique Queiroz Gil | Interface (tema, componentes, telas, acessibilidade) |

---

## O que o app faz

Lê uma base territorial local de 7.575 leituras horárias (água, energia, clima, resíduos e
serviços em 3 territórios), calcula indicadores, projeta risco para as **próximas 48 h** com um
método explicável e exibe **todo alerta junto da justificativa que o originou**. A decisão é
sempre humana: o app recomenda, a pessoa decide, e o registro fica no aparelho com responsável,
observação e data.

**Recorte principal: energia** — curva de carga e risco de sobrecarga no horário de pico.

### O que torna a entrega defensável

- **Nenhum alerta sem justificativa** — garantido pelo modelo de domínio: `Alert` não compila
  sem o campo `justification`.
- **O app também explica o silêncio** — quando um indicador não gera alerta, a tela diz por quê
  (histórico curto, dado vencido, tendência não identificável, projeção abaixo do limiar).
- **Degradação demonstrável** — quatro cenários de falha ligáveis em Configurações, sem recompilar.
- **Domínio sem Android** — o pacote `domain/` não tem um único `import android.*`; por isso os
  16 testes rodam na JVM em segundos.

---

## Como rodar

**Pré-requisitos:** Android Studio (Ladybug ou mais novo), JDK 17+, SDK do Android 36.

```bash
git clone <url-do-repositorio> && cd TerritoryIntelligenceDashboard
```

Gerar o APK release (instalável, assinado com a chave de debug):

```bash
./gradlew :app:assembleRelease
```

Rodar os 16 testes de unidade (motor de previsão + regressão do painel):

```bash
./gradlew :app:testDebugUnitTest
```

Instalar no aparelho ou emulador conectado:

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

Regenerar a base sintética (semente fixa — produz exatamente os mesmos dados):

```bash
python tools/gerar_dados_sinteticos.py
```

> O arquivo `local.properties` com `sdk.dir` é criado pelo Android Studio e **não** vai para o
> repositório.

---

## Arquitetura

```
app/src/main/java/br/com/fiap/tid/
├── TidApplication.kt          AppContainer — injeção de dependência manual
├── MainActivity.kt            única Activity; toda a UI é Compose
│
├── domain/                    KOTLIN PURO — zero import de Android
│   ├── model/                 Territory, Indicator, Reading, Alert, Justification, Decision
│   ├── forecast/              ForecastEngine — pico diário, média móvel, regressão linear
│   ├── usecase/               GetDashboard, GetIndicatorDetail, GetAlertDetail, RegisterDecision
│   ├── repository/            CONTRATO TerritoryRepository + DataFailureMode
│   └── platform/              CONTRATO AlertNotifier + NotifyResult
│
├── data/                      DETALHES SUBSTITUÍVEIS
│   ├── local/                 Room: TidDatabase, TidDao, 4 entidades
│   ├── seed/                  SeedLoader — carga inicial dos assets, em uma transação
│   ├── repository/            TerritoryRepositoryImpl + cenários de degradação
│   ├── prefs/                 PreferencesStore (SharedPreferences)
│   └── platform/              AndroidAlertNotifier — único ponto que conhece o Android nativo
│
└── ui/                        JETPACK COMPOSE
    ├── theme/                 fonte única de verdade visual (cores, tipografia, espaçamento)
    ├── components/            7 Composables reutilizáveis
    ├── common/                UiState (carregando, vazio, erro, sucesso) e formatadores
    ├── navigation/            TidNavHost — rotas carregam só identificadores
    └── screens/               5 telas, cada uma com seu ViewModel
```

**A direção das dependências aponta para o domínio.** A interface depende do domínio; a camada de
dados *implementa* contratos declarados no domínio. Trocar a base sintética por uma API pública
real é escrever outra implementação de `TerritoryRepository` e registrá-la no `AppContainer` —
nenhum caso de uso e nenhuma tela mudam.

### Telas

| Tela | Conteúdo |
|---|---|
| **Painel** | Seletor de território, origem e data da última atualização, alertas ativos por gravidade, indicadores e o motivo de cada indicador que não gerou alerta |
| **Detalhe do Indicador** | KPI, gráfico com histórico/limiares/projeção, fonte dos dados e justificativa |
| **Detalhe do Alerta** | Justificativa completa, confiança, recomendação, registro da decisão e notificação local |
| **Histórico de Decisões** | Ação, alerta, território, responsável, observação e data; exclusão (LGPD) |
| **Configurações** | Território, temas, limiares, permissão, fontes de dados, LGPD e simulação de falhas |

### Componentes reutilizáveis

`KpiCard` · `TrendChart` · `AlertCard` · `JustificationPanel` · `TerritorySelector` ·
`StateView` · `DecisionSheet`

Todos recebem dados por parâmetro e devolvem eventos por callback. Nenhum tem regra de negócio
dentro — o componente descreve, não decide.

---

## Como funciona a previsão

Em `domain/forecast/ForecastEngine.kt`, tudo em Kotlin puro e determinístico:

1. **Pico diário** — as leituras horárias são agregadas no maior valor de cada dia. O risco
   territorial acontece no pico (18h–19h na energia), não na média.
2. **Média móvel de 7 dias** e **variação semanal** (últimos 7 dias contra os 7 anteriores).
3. **Regressão linear** por mínimos quadrados sobre os picos de 14 dias, projetando para **+48 h**,
   guardando o R² como medida de aderência.
4. **Classificação** — projeção ≥ limiar crítico → `CRITICO`; ≥ limiar de atenção → `ATENCAO`;
   ≥ 90% do limiar de atenção → `INFORMATIVO`.
5. **Confiança** = `0,40 + 0,50 × R²`, com desconto se houver menos de 10 dias, limitada a 30–92%.

**O app NÃO emite alerta** (e diz o motivo na tela) quando: há menos de 8 dias de histórico; a
leitura mais recente tem mais de 24 h; a tendência não é identificável (R² < 0,20) e a projeção
está abaixo do limiar crítico; ou a projeção está abaixo de 90% do limiar de atenção.

> Não usamos um modelo de IA de propósito. O requisito é **apoio à decisão humana**: o gestor
> precisa poder conferir e contestar o alerta. Uma reta com R² declarado se confere com os números
> que a própria tela mostra.

---

## Dados

Base **100% sintética**, gerada por `tools/gerar_dados_sinteticos.py` com semente fixa (2026).
Licença: **uso acadêmico livre (GS2026.2)**.

Os valores foram calibrados para ficar na mesma ordem de grandeza de publicações abertas
(ONS — curva de carga; SNIS — saneamento; INMET — clima), mas **não reproduzem nenhuma série
real**. Nenhum dado pessoal, em nenhum ponto.

Os arquivos em `app/src/main/assets/seed/` usam **formato colunar** (uma série por par
indicador×território, com os valores em um vetor): 47 KB para as mesmas 7.575 leituras que
ocupavam 817 KB no formato orientado a registros.

Na tela, todo dado é identificado como fictício — no painel, em cada cartão, na justificativa, na
notificação e em Configurações.

---

## Degradação e recursos nativos

Configurações → *Simulação de falha* liga quatro cenários sem recompilar:

| Modo | O que o app faz |
|---|---|
| Normal | Leituras locais disponíveis e atualizadas |
| Simular dados ausentes | Estado vazio com o motivo e botão "Tentar novamente" |
| Simular dados desatualizados | Faixa de aviso e **alertas preditivos suspensos** |
| Simular falha de leitura | Mensagem clara de erro e botão "Tentar novamente" |

**Notificação local** (recurso nativo): a permissão é pedida **no toque que precisa dela**, não na
abertura do app. Os três caminhos estão tratados — concedida (`Sent`), negada
(`PermissionDenied`, com plano B na tela e atalho para as configurações do sistema) e instável
(`Unavailable`). Nenhum alerta se perde: ele permanece visível no painel.

---

## Qualidade

| Prova | Meta | Resultado medido |
|---|---|---|
| Painel pronto | ≤ 2.000 ms | 341 ms na 1ª abertura; 22 ms nas seguintes |
| Carga inicial | ≤ 1.000 ms | 303 ms (mediana) para 7.575 leituras |
| Rolagem | < 5% de quadros perdidos | 3,57%; p95 = 18 ms (release) |
| Contraste | ≥ 4,5:1 | pior par 5,48:1 (17 pares medidos, claro e escuro) |
| Fonte a 200% | sem corte | aprovado |
| Testes de unidade | 100% verdes | 16/16 |

Medições no emulador Pixel API 36 com o **APK release**. As marcas `TID_PERF` são gravadas no
logcat pelo próprio app. Os números foram **revalidados em 18/11/2026** a partir de uma
instalação limpa do APK entregue, em emulador acelerado por WHPX em vez de AEHD: carga inicial
313 ms, painel 367 ms na 1ª abertura e 20 ms nas seguintes. Evidências completas (21 capturas) em `docs/evidencias/`.

---

## Privacidade (LGPD)

O app funciona offline e guarda **apenas no próprio aparelho**: as leituras sintéticas, o
território escolhido, os temas monitorados e o histórico de decisões (nome do responsável, ação,
observação e data). Nada é enviado para servidores — não há servidor. O usuário apaga o histórico
a qualquer momento, em Configurações ou no Histórico. `allowBackup=false`: desinstalar remove tudo.

---

## Entrega

| Arquivo | Conteúdo |
|---|---|
| `entrega/GS2026.2-TerritoryIntelligenceDashboard.pdf` | Documentação completa das 8 etapas (29 páginas) |
| `entrega/TerritoryIntelligenceDashboard-v1.0.0-release.apk` | Build instalável (7,46 MB) |
| `docs/DOCUMENTACAO_GS2026.2.html` | Fonte do PDF |
| [Vídeo demonstrativo no YouTube](https://youtube.com/shorts/3ebA_WhBFpw) | 2 min 48 s, sem narração (o `.mp4` fica fora do repositório por tamanho) |
| `docs/evidencias/` | 21 capturas do app em execução |
| `tools/gerar_dados_sinteticos.py` | Gerador da base, com origem, licença e finalidade |
