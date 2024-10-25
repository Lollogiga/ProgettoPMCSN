import os
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import numpy as np
from scipy.stats import pareto, expon, gamma, lognorm, weibull_min, beta

matplotlib.use('Agg')

def plot_finite_graph(file_path, selected_seeds, img_folder, img_name, server_name):
    # Carica i dati dal file CSV
    df = pd.read_csv(
        file_path,
        header=None,
        names=['Run', 'Seed', 'Center', 'Time', 'E[T_S]', 'E[N_S]', 'E[T_Q]', 'E[N_Q]'],
        dtype={'Run': 'int', 'Seed': 'int64', 'Center': 'str', 'Time': 'double', 'E[T_S]': 'double', 'E[N_S]': 'double', 'E[T_Q]': 'double', 'E[N_Q]': 'double'},
        skiprows=1,
        low_memory=False
    )
    df = df[df['Seed'].isin(selected_seeds)]
    df.dropna(inplace=True)
    df = df[~df.isin([np.inf, -np.inf]).any(axis=1)]

    # Imposta il limite di tempo a 86400 secondi (24 ore)
    df = df[df['Time'] <= 86400]

    # Crea il grafico unico
    plt.figure(figsize=(12, 8))

    # Filtra per ogni Run e aggiungi la linea al grafico
    unique_seeds = df['Seed'].unique()
    for seed in unique_seeds:
        df_run = df[df['Seed'] == seed]
        # Calcola i tempi di risposta, iniziando da 0
        response_time = [0] + (df_run['E[T_S]']/60).tolist()  # Aggiungi 0 all'inizio
        # Crea un nuovo asse X che inizia da 0 e ha la stessa lunghezza di response_time
        time_values = [0] + (df_run['Time']/60).tolist()  # Aggiungi 0 all'inizio
        # Plot dei dati
        plt.plot(time_values, response_time, marker='.', linestyle='-', markersize=4, label=seed)


    # Configura l'aspetto del grafico
    plt.xlabel('Time (min)')
    plt.ylabel('E[T_S] (min)')
    plt.title('Time vs E[T_S] for ' + server_name)
    plt.grid(True)
    plt.legend()

    # Crea la cartella se non esiste
    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

def plot_infinite_graph(file_path, img_folder, img_name, server_name):
    # Carica i dati dal file CSV
    df = pd.read_csv(
        file_path,
        header=None,
        names=['Batch Number', 'E[T_S]'],
        dtype={'Batch Number': 'int', 'E[T_S]': 'double'},
        skiprows=1,
        low_memory=False
    )

    # Verifica che ci siano almeno 64 righe
    if len(df) < 128:
        raise ValueError("Il file CSV deve contenere almeno 128 righe.")

    # Crea il grafico unico
    plt.figure(figsize=(12, 8))

    response_time = []
    cumulative_sum = 0

    # Calcola la media cumulativa per i primi 64 batch
    for i in range(128):
        cumulative_sum += df.at[i, 'E[T_S]']
        if i == 0:
            mean = cumulative_sum / (i + 1)  # i+1 per evitare divisione per zero
        else:
            mean = cumulative_sum / i
            mean /= 60
        response_time.append(mean)

    batch_number_reduced = df['Batch Number'][:128:2]
    response_time_reduced = response_time[::2]

    # Disegna il grafico
    plt.plot(batch_number_reduced, response_time_reduced, marker='.', linestyle='-', markersize=8, zorder=2)
    plt.yscale('log')

    theoretical_values = {
        "Ricarica": 45.07377,
        "Noleggio": 4.99576,
        "Strada": 30.03523,
        "Parcheggio": 1.59983
    }

    if server_name in theoretical_values:
        plt.axhline(y=theoretical_values[server_name], color='red', linestyle='--', zorder=1)

    # Configura l'aspetto del grafico
    plt.xlabel('Batch Index')
    plt.ylabel('E[T_S] (min)')
    plt.title('Batch index vs E[T_S] for ' + server_name)
    plt.grid(True)

    # Crea la cartella se non esiste
    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    # Salva il grafico come immagine
    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

def plot_histogram(file_csv, img_folder, img_name):
    df = pd.read_csv(file_csv)

    df_sorted = df.sort_values(by='Time')

    # Calcola gli inter-arrivi
    inter_arrivi = df_sorted['Time'].diff().dropna()

    # Numero totale di campioni
    num_campioni = len(inter_arrivi)

    # Crea l'istogramma dei dati
    plt.hist(inter_arrivi, bins=30, density=True, alpha=0.6, color='g', label=f'Numero totale di campioni: {num_campioni}')
    plt.title('Istogramma dei tempi di inter-arrivo')
    plt.xlabel('Tempo inter-arrivo ')
    plt.ylabel('Densità')
    plt.legend()  # Aggiunge la legenda

    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    # Salva l'immagine nella cartella specificata
    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()

def plot_distributions(file_csv, exponParam, paretoParam, gammaParam, lognormParam, weibullParam, betaParam, img_folder, img_name):
    # Legge il file CSV
    df = pd.read_csv(file_csv)

    # Ordina i dati per il tempo
    df_sorted = df.sort_values(by='Time')

    # Calcola le differenze tra i tempi consecutivi (inter-arrivi)
    differenze_tempo = df_sorted['Time'].diff().dropna()

    # Plot dell'istogramma (usando plt.hist, che è corretto)
    plt.figure(figsize=(10, 6))  # Definisce la dimensione della finestra di plot
    plt.hist(differenze_tempo, bins=30, density=True, alpha=0.6, color='g')

    # Esponenziale
    loc_expon = exponParam[0]
    scale_expon = exponParam[1]

    # Calcola i valori della curva Esponenziale
    x = np.linspace(differenze_tempo.min(), differenze_tempo.max(), 100)
    y_expon = expon.pdf(x, loc=loc_expon, scale=scale_expon)
    plt.plot(x, y_expon, '-', color="orange", lw=2, label='Distribuzione Esponenziale')

    # Pareto
    shape_pareto = paretoParam[0]
    loc_pareto = paretoParam[1]
    scale_pareto = paretoParam[2]

    y = pareto.pdf(x, shape_pareto, loc=loc_pareto, scale=scale_pareto)

    plt.plot(x, y, '-', color="purple", lw=3, label='Distribuzione Pareto')

    # Gamma
    a_gamma = gammaParam[0]
    loc_gamma = gammaParam[1]
    scale_gamma = gammaParam[2]

    # Calcola i valori della curva Gamma
    x = np.linspace(1.5, differenze_tempo.max(), 100)
    y_gamma = gamma.pdf(x, a_gamma, loc=loc_gamma, scale=scale_gamma)
    plt.plot(x, y_gamma, '-', color="#1e81b0", lw=2, label='Distribuzione Gamma')

    # Lognorm
    shape_lognorm = lognormParam[0]
    loc_lognorm = lognormParam[1]
    scale_lognorm = lognormParam[2]

    y_lognorm = lognorm.pdf(x, shape_lognorm, loc=loc_lognorm, scale=scale_lognorm)
    plt.plot(x, y_lognorm, '-', color="red", lw=2, label='Distribuzione Lognormale')

    # Weibull
    shape_weibull = weibullParam[0]
    loc_weibull = weibullParam[1]
    scale_weibull = weibullParam[2]

    y_weibull = weibull_min.pdf(x, shape_weibull, loc=loc_weibull, scale=scale_weibull)
    plt.plot(x, y_weibull, '-', color="blue", lw=2, label='Distribuzione Weibull')

    # Beta
    a_beta = betaParam[0]
    b_beta = betaParam[1]
    loc_beta = betaParam[2]
    scale_beta = betaParam[3]

    y_beta = beta.pdf(x, a_beta, b_beta, loc=loc_beta, scale=scale_beta)
    plt.plot(x, y_beta, '-', color="green", lw=2, label='Distribuzione Beta')

    plt.title('Istogramma dei tempi di inter-arrivo')
    plt.xlabel('Tempo inter-arrivo')
    plt.ylabel('Densità')
    plt.legend()

    if not os.path.exists(img_folder):
        os.makedirs(img_folder)

    output_path = os.path.join(img_folder, img_name)
    plt.savefig(output_path)
    plt.close()