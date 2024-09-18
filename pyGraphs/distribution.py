import os
import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import pandas as pd
from scipy import stats
from fitter import Fitter

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
    plt.title('Istogramma dei tempi inter-arrivi')
    plt.xlabel('Tempo inter-arrivi')
    plt.ylabel('Densità')

    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

    # Fit di una distribuzione esponenziale sui dati
    lambda_exp = 1 / differenze_tempo.mean()
    print(f"Stima di lambda (esponenziale): {lambda_exp}")

    # Verifica se segue una distribuzione esponenziale
    kstest_result = stats.kstest(differenze_tempo, 'expon', args=(0, differenze_tempo.mean()))
    print(f"Test di Kolmogorov-Smirnov per distribuzione esponenziale: {kstest_result}")

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
    f = Fitter(inter_arrivi, distributions=['gamma', 'expon', 'lognorm', 'weibull_min'])
    f.fit()
    f.summary()

    # Stampa i parametri stimati per le distribuzioni migliori
    print(f.fitted_param)