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