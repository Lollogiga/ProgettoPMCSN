import os
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import numpy as np

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