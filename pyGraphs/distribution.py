import matplotlib
import pandas as pd
from fitter import Fitter
from scipy import stats

matplotlib.use('Agg')


def calcola_tasso_arrivo_medio(file_csv):
    # Legge il file CSV
    df = pd.read_csv(file_csv)

    # Raggruppa per 'Seed' per considerare diverse esecuzioni
    grouped = df.groupby('Seed')

    # Inizializza una lista per memorizzare i tassi di arrivo medi per ogni seed
    tassi_arrivo = []

    # Per ogni gruppo (ogni seed), calcola il tasso di arrivo
    for seed, group in grouped:
        # Ordina i tempi di arrivo
        group_sorted = group.sort_values(by='Time')

        # Calcola le differenze tra i tempi consecutivi
        differenze_tempo = group_sorted['Time'].diff().dropna()

        # Calcola il tasso di arrivo (1 / differenza media)
        tasso_arrivo_medio = 1 / differenze_tempo.mean()
        tassi_arrivo.append(tasso_arrivo_medio)

    # Restituisce la media dei tassi di arrivo
    return sum(tassi_arrivo) / len(tassi_arrivo) if tassi_arrivo else 0


def fitterAnalyses(file_csv, fitterDistributions, verbose=True):
    df = pd.read_csv(file_csv)

    df_sorted = df.sort_values(by='Time')

    # Calcola gli inter-arrivi
    inter_arrivi = df_sorted['Time'].diff().dropna()

    if not verbose:
        return

    f = Fitter(inter_arrivi, distributions=fitterDistributions)
    f.fit()
    f.summary()

    # Stampa i parametri stimati per le distribuzioni migliori
    for distr in f.fitted_param:
        print(f"{distr}, {f.fitted_param[distr]}")


def test_kolmogorov_smirnov(file_csv, distr_list, distr_args, verbose=True):
    # Legge il file CSV
    df = pd.read_csv(file_csv)

    # Ordina i dati per il tempo
    df_sorted = df.sort_values(by='Time')

    # Calcola le differenze tra i tempi consecutivi (inter-arrivi)
    differenze_tempo = df_sorted['Time'].diff().dropna()

    ks_results = {}

    # Stampa i risultati
    if not verbose:
        return

    for i in range(len(distr_list)):
        kstest_result = stats.kstest(differenze_tempo, distr_list[i], args=distr_args[i])

        ks_results[distr_list[i]] = kstest_result

    sorted_ks_results = sorted(ks_results.items(), key=lambda item: item[1].pvalue, reverse=True)

    max_length = max(len(distr) for distr, _ in sorted_ks_results)
    for distr, result in sorted_ks_results:
        print(f"Test K-S: [{distr:<{max_length}}], p-value= {result[1]}")