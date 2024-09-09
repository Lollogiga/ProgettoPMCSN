import os
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt

matplotlib.use('Agg')  # Usa il backend non interattivo 'Agg'

def extract_data_from_file(file_path):
    """
    Estrae i dati da un file CSV e restituisce un DataFrame con le informazioni.
    """
    try:
        df = pd.read_csv(file_path, na_values=['', ' '])
        df.iloc[:, 1:] = df.iloc[:, 1:].apply(pd.to_numeric, errors='coerce')
        return df
    except Exception as e:
        print(f"Errore nella lettura del file {file_path}: {e}")
        return None

def plot_metric_with_given_x(x_values, df, metric, centers, output_path):
    """
    Plotta i valori di una metrica specifica per diversi centri e salva il grafico.
    """
    plt.figure()  # Crea una nuova figura
    for center in centers:
        filtered_data = df[df['Center'] == center].copy()  # Usa .copy() per evitare l'avviso di SettingWithCopyWarning

        # Riempie i valori mancanti con zero per 'Strada'
        if center == 'Strada':
            filtered_data[metric] = filtered_data[metric].fillna(0)

        y_values = filtered_data[metric].dropna().values  # Rimuove i valori NaN

        if len(x_values) != len(y_values):
            x_values = [1] * len(y_values)  # Assicurati che x_values e y_values abbiano la stessa lunghezza

        plt.plot(x_values, y_values, marker='o', label=center)

    plt.xlabel('Tempo')
    plt.ylabel(metric)
    plt.title(f'Grafico di {metric} per diversi centri')
    plt.legend()
    plt.grid(True)

    # Salva e chiudi la figura
    try:
        plt.savefig(output_path)
        print(f"Grafico salvato come {output_path}")
    except Exception as e:
        print(f"Errore nel salvataggio del grafico {output_path}: {e}")
    finally:
        plt.close()  # Chiude la figura corrente

def plot_metrics(folder):
    """
    Genera e salva grafici per tutte le metriche nei file CSV della cartella di input.
    """
    # folder = os.path.join(input_folder, 'resources', 'results')

    if not os.path.exists(folder):
        print(f"La cartella {folder} non esiste.")
        return

    for seed_folder in os.listdir(folder):
        seed_path = os.path.join(folder, seed_folder)

        if os.path.isdir(seed_path):
            graphic_folder = os.path.join(seed_path, 'dirNameGraphic')
            os.makedirs(graphic_folder, exist_ok=True)  # Crea la sottocartella se non esiste

            for file_name in os.listdir(seed_path):
                file_path = os.path.join(seed_path, file_name)

                if not os.path.isdir(file_path):  # Controlla che non sia una directory
                    df = extract_data_from_file(file_path)

                    if df is not None:
                        centers = df['Center'].unique()
                        metrics = df.columns[1:]  # Le colonne delle metriche

                        for metric in metrics:
                            output_path = os.path.join(graphic_folder, f'{seed_folder}_{metric}.png')
                            plot_metric_with_given_x([1], df, metric, centers, output_path)


def delete_all_graphics(input_folder):
    """
    Elimina tutti i grafici salvati nella sottocartella 'dirNameGraphic' all'interno di ogni cartella seed.
    """
    result_folder = os.path.join(input_folder, 'resources', 'results')

    if not os.path.exists(result_folder):
        print(f"La cartella {result_folder} non esiste.")
        return

    for seed_folder in os.listdir(result_folder):
        seed_path = os.path.join(result_folder, seed_folder)

        if os.path.isdir(seed_path):
            graphic_folder = os.path.join(seed_path, 'dirNameGraphic')

            if os.path.isdir(graphic_folder):
                for file_name in os.listdir(graphic_folder):
                    file_path = os.path.join(graphic_folder, file_name)

                    if os.path.isfile(file_path):
                        try:
                            os.remove(file_path)
                            print(f"Grafico eliminato: {file_path}")
                        except Exception as e:
                            print(f"Errore nell'eliminazione del file {file_path}: {e}")

def plot_combined_graph(file_path, img_name):
    # Carica i dati dal file CSV
    df = pd.read_csv(file_path, header=None, names=['Run', 'Center', 'Time', 'E[T_S]', 'E[N_S]', 'E[T_Q]', 'E[N_Q]'])

    # Converti le colonne in tipo float
    df['Time'] = pd.to_numeric(df['Time'], errors='coerce')
    df['E[T_S]'] = pd.to_numeric(df['E[T_S]'], errors='coerce')
    df['E[N_S]'] = pd.to_numeric(df['E[N_S]'], errors='coerce')
    df['E[T_Q]'] = pd.to_numeric(df['E[T_Q]'], errors='coerce')
    df['E[N_Q]'] = pd.to_numeric(df['E[N_Q]'], errors='coerce')

    # Imposta il limite di tempo a 86400 secondi (24 ore)
    df = df[df['Time'] <= 86400]

    # Crea il grafico unico
    plt.figure(figsize=(12, 8))

    # Filtra per ogni Run e aggiungi la linea al grafico
    unique_runs = df['Run'].unique()
    for run in unique_runs:
        df_run = df[df['Run'] == run]
        plt.plot(df_run['Time'], df_run['E[T_S]'], marker='o', linestyle='-', markersize=4, label=f'Run {run}')

    # Configura l'aspetto del grafico
    plt.xlabel('Time (s)')
    plt.ylabel('E[T_S]')
    plt.title('Time vs E[T_S] for Different Runs')
    plt.grid(True)

    plt.savefig(img_name)

# Esempio di utilizzo
input_folder = "D:/path/to/your/folder"
plot_metrics(input_folder)
#delete_all_graphics(input_folder)
