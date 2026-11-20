# -*- coding: utf-8 -*-
"""
Territory Intelligence Dashboard - GS2026.2 (FIAP)
Gerador da base SINTETICA de leituras territoriais.

FINALIDADE
    Produzir os arquivos JSON consumidos pelo app em `app/src/main/assets/seed/`
    para popular o banco Room na primeira execucao.

ORIGEM E LICENCA DOS DADOS
    100% SINTETICOS, gerados por este script. Nenhum dado pessoal e nenhum dado
    real de concessionaria foi utilizado. Licenca: uso academico livre (GS2026.2).
    Os valores foram calibrados para ficar na mesma ordem de grandeza de
    publicacoes abertas (ONS - curva de carga; SNIS - saneamento; INMET - clima),
    mas NAO reproduzem nenhuma serie real.

REPRODUTIBILIDADE
    Semente fixa (SEED = 2026). Rodar o script de novo gera exatamente o mesmo
    conjunto de dados.

USO
    python tools/gerar_dados_sinteticos.py
"""

import json
import math
import os
import random
from datetime import datetime, timedelta, timezone

SEED = 2026
DIAS = 21              # historico gerado
PASSO_HORAS = 1        # uma leitura por hora
ORIGEM = "SINTETICA"

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DESTINO = os.path.join(RAIZ, "app", "src", "main", "assets", "seed")

# ---------------------------------------------------------------- territorios
TERRITORIOS = [
    {"id": "t-jardim-flores", "name": "Jardim das Flores", "type": "BAIRRO",
     "latitude": -23.5880, "longitude": -46.6320},
    {"id": "t-centro", "name": "Centro", "type": "BAIRRO",
     "latitude": -23.5505, "longitude": -46.6333},
    {"id": "t-bom-retiro-rural", "name": "Sitio Bom Retiro", "type": "AREA_RURAL",
     "latitude": -23.7210, "longitude": -46.8450},
]

# ----------------------------------------------------------------- indicadores
# attentionThreshold / criticalThreshold sao os limiares de referencia.
INDICADORES = [
    {"id": "i-energia", "theme": "ENERGIA", "name": "Carga de energia",
     "unit": "kWh/h", "attentionThreshold": 900.0, "criticalThreshold": 1150.0,
     "description": "Curva de carga do alimentador que atende o territorio."},
    {"id": "i-agua", "theme": "AGUA", "name": "Vazao de agua distribuida",
     "unit": "L/s", "attentionThreshold": 48.0, "criticalThreshold": 58.0,
     "description": "Vazao media distribuida no territorio."},
    {"id": "i-clima", "theme": "CLIMA", "name": "Temperatura do ar",
     "unit": "C", "attentionThreshold": 34.0, "criticalThreshold": 38.0,
     "description": "Temperatura horaria; calor extremo pressiona agua e energia."},
    {"id": "i-residuos", "theme": "RESIDUOS", "name": "Residuos acumulados",
     "unit": "t", "attentionThreshold": 18.0, "criticalThreshold": 24.0,
     "description": "Estoque de residuos aguardando coleta no ponto critico."},
    {"id": "i-servicos", "theme": "SERVICOS", "name": "Tempo de espera no atendimento",
     "unit": "min", "attentionThreshold": 35.0, "criticalThreshold": 50.0,
     "description": "Tempo medio de espera na unidade de atendimento."},
]

# Perfis por (indicador, territorio):
#   base            -> valor medio inicial
#   crescimento_sem -> crescimento percentual por semana (tendencia)
#   amplitude_dia   -> amplitude do ciclo diario (fracao da base)
#   hora_pico       -> hora do pico diario
#   ruido           -> desvio padrao do ruido (fracao da base)
PERFIS = {
    # ENERGIA: Jardim das Flores em tendencia de alta -> gera alerta de ATENCAO
    ("i-energia", "t-jardim-flores"):     dict(base=555, crescimento_sem=0.115, amplitude_dia=0.34, hora_pico=19, ruido=0.030),
    ("i-energia", "t-centro"):            dict(base=700, crescimento_sem=0.015, amplitude_dia=0.22, hora_pico=15, ruido=0.030),
    ("i-energia", "t-bom-retiro-rural"):  dict(base=180, crescimento_sem=0.010, amplitude_dia=0.18, hora_pico=20, ruido=0.040),
    # CLIMA: onda de calor no Jardim das Flores -> gera alerta CRITICO
    ("i-clima", "t-jardim-flores"):       dict(base=26.0, crescimento_sem=0.075, amplitude_dia=0.26, hora_pico=15, ruido=0.015),
    ("i-clima", "t-centro"):              dict(base=25.0, crescimento_sem=0.020, amplitude_dia=0.24, hora_pico=15, ruido=0.015),
    ("i-clima", "t-bom-retiro-rural"):    dict(base=23.0, crescimento_sem=0.012, amplitude_dia=0.30, hora_pico=14, ruido=0.020),
    # AGUA: estavel -> sem alerta
    ("i-agua", "t-jardim-flores"):        dict(base=33.0, crescimento_sem=0.012, amplitude_dia=0.18, hora_pico=8,  ruido=0.025),
    ("i-agua", "t-centro"):               dict(base=38.0, crescimento_sem=0.008, amplitude_dia=0.16, hora_pico=8,  ruido=0.025),
    ("i-agua", "t-bom-retiro-rural"):     dict(base=12.0, crescimento_sem=0.005, amplitude_dia=0.22, hora_pico=7,  ruido=0.035),
    # RESIDUOS: leve alta -> alerta INFORMATIVO
    ("i-residuos", "t-jardim-flores"):    dict(base=11.0, crescimento_sem=0.055, amplitude_dia=0.28, hora_pico=22, ruido=0.030),
    ("i-residuos", "t-centro"):           dict(base=13.0, crescimento_sem=0.048, amplitude_dia=0.26, hora_pico=22, ruido=0.030),
    ("i-residuos", "t-bom-retiro-rural"): dict(base=4.0,  crescimento_sem=0.010, amplitude_dia=0.30, hora_pico=21, ruido=0.045),
    # SERVICOS: estavel -> sem alerta
    ("i-servicos", "t-jardim-flores"):    dict(base=21.0, crescimento_sem=0.018, amplitude_dia=0.30, hora_pico=10, ruido=0.040),
    ("i-servicos", "t-centro"):           dict(base=26.0, crescimento_sem=0.010, amplitude_dia=0.28, hora_pico=10, ruido=0.040),
    ("i-servicos", "t-bom-retiro-rural"): dict(base=15.0, crescimento_sem=0.006, amplitude_dia=0.32, hora_pico=9,  ruido=0.050),
}


def gerar_serie(perfil, inicio, total_horas, rnd):
    """Serie = base * tendencia semanal * ciclo diario * fator semana * ruido."""
    pontos = []
    for h in range(0, total_horas, PASSO_HORAS):
        instante = inicio + timedelta(hours=h)
        semanas = h / (24.0 * 7.0)
        tendencia = (1.0 + perfil["crescimento_sem"]) ** semanas
        fase = (instante.hour - perfil["hora_pico"]) / 24.0 * 2.0 * math.pi
        ciclo = 1.0 + perfil["amplitude_dia"] * math.cos(fase)
        fator_semana = 0.93 if instante.weekday() >= 5 else 1.0
        ruido = rnd.gauss(0.0, perfil["ruido"])
        valor = perfil["base"] * tendencia * ciclo * fator_semana * (1.0 + ruido)
        pontos.append((instante, round(valor, 2)))
    return pontos


def gravar(caminho, dados):
    with open(caminho, "w", encoding="utf-8") as f:
        json.dump(dados, f, ensure_ascii=False, separators=(",", ":"))


def main():
    rnd = random.Random(SEED)
    # Ancora fixa para a base ser reproduzivel. O app converte "hoursAgo" em
    # timestamp real no momento da carga, por isso os dados nunca nascem velhos.
    fim = datetime(2026, 11, 10, 23, 0, 0, tzinfo=timezone.utc)
    inicio = fim - timedelta(days=DIAS)
    total_horas = DIAS * 24 + 1

    # FORMATO COLUNAR: uma serie por (indicador, territorio), com os valores
    # em um array. Evita repetir as chaves 7.500 vezes - o arquivo fica ~5x
    # menor e o parse no aparelho fica muito mais barato (ver plano de
    # evidencias de qualidade, Prova 1).
    series = []
    total_leituras = 0
    for ind in INDICADORES:
        for ter in TERRITORIOS:
            perfil = PERFIS[(ind["id"], ter["id"])]
            pontos = gerar_serie(perfil, inicio, total_horas, rnd)
            valores = [v for _, v in pontos]
            primeiro_hours_ago = int(round((fim - pontos[0][0]).total_seconds() / 3600.0))
            series.append({
                "indicatorId": ind["id"],
                "territoryId": ter["id"],
                "origin": ORIGEM,
                "startHoursAgo": primeiro_hours_ago,
                "stepHours": PASSO_HORAS,
                "values": valores,
            })
            total_leituras += len(valores)

    os.makedirs(DESTINO, exist_ok=True)
    gravar(os.path.join(DESTINO, "territories.json"), TERRITORIOS)
    gravar(os.path.join(DESTINO, "indicators.json"), INDICADORES)
    gravar(os.path.join(DESTINO, "readings.json"), series)

    tamanho = os.path.getsize(os.path.join(DESTINO, "readings.json"))
    print("Territorios:", len(TERRITORIOS))
    print("Indicadores:", len(INDICADORES))
    print("Series     :", len(series))
    print("Leituras   :", total_leituras)
    print("readings.json:", f"{tamanho / 1024:.1f} KB")
    print("Destino    :", DESTINO)


if __name__ == "__main__":
    main()
