from graphicDraw import plot_combined_graph

def finiteSimGraphs():
    finiteNoleggio = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\finiteNoleggio.csv"
    finiteStrada = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\finiteStrada.csv"
    finiteParcheggio = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\finiteParcheggio.csv"
    finiteRicarica = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\finiteRicarica.csv"

    plot_combined_graph(finiteNoleggio, "noleggio.png")
    plot_combined_graph(finiteStrada,"strada.png")
    plot_combined_graph(finiteParcheggio, "parcheggio.png")
    plot_combined_graph(finiteRicarica, "ricarica.png")

def main():
    finiteSimGraphs()

    # infiniteSimFolder = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\infinite_horizon"


if __name__ == "__main__":
    main()

