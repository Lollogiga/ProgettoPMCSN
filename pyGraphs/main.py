import os
from graphicDraw import plot_combined_graph

baseFolder = os.path.abspath(os.path.join(os.getcwd(), os.pardir))

resultsPath = baseFolder + "\\" + "resources\\results\\"
finiteSimFolder = "finiteSimImg\\"

def finiteSimGraphs():
    finiteNoleggio = resultsPath + "finiteNoleggio.csv"
    finiteStrada = resultsPath + "finiteStrada.csv"
    finiteParcheggio = resultsPath + "finiteParcheggio.csv"
    finiteRicarica = resultsPath + "finiteRicarica.csv"

    plot_combined_graph(finiteNoleggio, resultsPath + finiteSimFolder, "noleggio.png")
    plot_combined_graph(finiteStrada, resultsPath + finiteSimFolder, "strada.png")
    plot_combined_graph(finiteParcheggio, resultsPath + finiteSimFolder, "parcheggio.png")
    plot_combined_graph(finiteRicarica, resultsPath + finiteSimFolder, "ricarica.png")

def main():
    finiteSimGraphs()

    # infiniteSimFolder = "D:\\Projects\\IdeaProjects\\ProgettoPMCSN\\resources\\results\\infinite_horizon"


if __name__ == "__main__":
    main()

