import os

import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import pandas as pd
from fitter import Fitter
from scipy import stats
from scipy.stats import pareto, expon, gamma
from six import print_

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


def exponentialAnalyses(file_csv, img_folder, img_name):
    # Legge il file CSV
    df = pd.read_csv(file_csv)

    # Ordina i dati per il tempo
    df_sorted = df.sort_values(by='Time')

    # Calcola le differenze tra i tempi consecutivi (inter-arrivi)
    differenze_tempo = df_sorted['Time'].diff().dropna()

    # Plot dell'istogramma (usando plt.hist, che è corretto)
    plt.figure(figsize=(10, 6))  # Definisce la dimensione della finestra di plot
    plt.hist(differenze_tempo, bins=30, density=True, alpha=0.6, color='g')

    # Pareto
    shape_pareto = 102.26194559016169
    loc_pareto = -5871.262526101017
    scale_pareto = 5871.2683699696845

    x = np.linspace(differenze_tempo.min(), differenze_tempo.max(), 100)
    y = pareto.pdf(x, shape_pareto, loc=loc_pareto, scale=scale_pareto)

    plt.plot(x, y, '-', color="purple", lw=3, label='Distribuzione Pareto')

    # Esponenziale
    loc_expon = 0.00584386866830755
    scale_expon = 57.98148749778005

    # Calcola i valori della curva Esponenziale
    y_expon = expon.pdf(x, loc=loc_expon, scale=scale_expon)
    plt.plot(x, y_expon, '-', color="orange", lw=2, label='Distribuzione Esponenziale')

    # Gamma
    a_gamma = 0.9574258203594337
    loc_gamma = 0.0058438686683070385
    scale_gamma = 64.7425984473532

    # Calcola i valori della curva Gamma
    x = np.linspace(1.5, differenze_tempo.max(), 100)
    y_gamma = gamma.pdf(x, a_gamma, loc=loc_gamma, scale=scale_gamma)
    plt.plot(x, y_gamma, '-', color="#1e81b0", lw=2, label='Distribuzione Gamma')

    plt.title('Istogramma dei tempi di inter-arrivo')
    plt.xlabel('Tempo inter-arrivo')
    plt.ylabel('Densità')
    plt.legend()

    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

def testKolmogorovSmirnov(file_csv, verbose=True):
    # Legge il file CSV
    df = pd.read_csv(file_csv)

    # Ordina i dati per il tempo
    df_sorted = df.sort_values(by='Time')

    # Calcola le differenze tra i tempi consecutivi (inter-arrivi)
    differenze_tempo = df_sorted['Time'].diff().dropna()

    distribuzioni = ['expon', 'norm', 'lognorm', 'gamma', 'chi2', 'pareto', 'weibull_min', 'beta', 'gumbel_r', 'levy']

    ks_results = {}

    for distribuzione in distribuzioni:
        if distribuzione == 'expon':
            # Per la distribuzione esponenziale, usiamo la media dei tempi inter-arrivo come parametro
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(0, differenze_tempo.mean()))
        elif distribuzione == 'norm':
            # Per la distribuzione normale, usiamo la media e la deviazione standard
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(differenze_tempo.mean(), differenze_tempo.std()))
        elif distribuzione == 'lognorm':
            # Per la lognormale, stimiamo shape, loc, e scale dai dati
            shape, loc, scale = stats.lognorm.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(shape, loc, scale))
        elif distribuzione == 'gamma':
            # Per la gamma, stimiamo shape, loc, e scale
            shape, loc, scale = stats.gamma.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(shape, loc, scale))
        elif distribuzione == 'chi2':
            # Per chi-quadro, stimiamo df, loc, e scale
            df, loc, scale = stats.chi2.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(df, loc, scale))
        elif distribuzione == 'pareto':
            # Per la pareto, stimiamo b, loc, e scale
            b, loc, scale = stats.pareto.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(b, loc, scale))
        elif distribuzione == 'weibull_min':
            # Per la weibull_min, stimiamo c, loc, e scale
            c, loc, scale = stats.weibull_min.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(c, loc, scale))
        elif distribuzione == 'beta':
            # Per la beta, stimiamo a, b, loc, e scale
            a, b, loc, scale = stats.beta.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(a, b, loc, scale))
        elif distribuzione == 'gumbel_r':
            # Per la gumbel_r, stimiamo loc e scale
            loc, scale = stats.gumbel_r.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(loc, scale))
        elif distribuzione == 'levy':
            # Per la levy, stimiamo loc e scale
            loc, scale = stats.levy.fit(differenze_tempo)
            kstest_result = stats.kstest(differenze_tempo, distribuzione, args=(loc, scale))

        # Salva il risultato del K-S test
        ks_results[distribuzione] = kstest_result

        # Stampa i risultati
        if not verbose:
            return

        for distribuzione, result in ks_results.items():
            print(f"Test di Kolmogorov-Smirnov per distribuzione {distribuzione}: {result}")

def fitterAnalyses(file_csv, img_folder, img_name):
    df = pd.read_csv(file_csv)

    df_sorted = df.sort_values(by='Time')

    # Calcola gli inter-arrivi
    inter_arrivi = df_sorted['Time'].diff().dropna()

    # Istogramma dei dati
    plt.hist(inter_arrivi, bins=30, density=True, alpha=0.6, color='g')
    plt.title('Istogramma dei tempi inter-arrivi')
    plt.xlabel('Tempo inter-arrivi')
    plt.ylabel('Densità')

    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

    # Confronta diverse distribuzioni
    fitterDistributions = ["cauchy", "chi2", "expon", "exponpow", "gamma", "lognorm", "norm", "pareto", "powerlaw", "rayleigh", "uniform"]

    # f = Fitter(inter_arrivi, distributions=['gamma', 'expon', 'lognorm', 'weibull_min'])
    f = Fitter(inter_arrivi, distributions=fitterDistributions)
    f.fit()
    f.summary()

    # Stampa i parametri stimati per le distribuzioni migliori
    for distr in f.fitted_param:
        print(f"{distr}, {f.fitted_param[distr]}")
