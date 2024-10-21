/* -------------------------------------------------------------------------
 * This program is based on a one-pass algorithm for the calculation of an
 * array of autocorrelations r[1], r[2], ... r[K].  The key feature of this
 * algorithm is the circular array 'hold' which stores the (K + 1) most
 * recent data points and the associated index 'p' which points to the
 * (rotating) head of the array.
 *
 * Data is read from a text file in the format 1-data-point-per-line (with
 * no blank lines).  Similar to programs UVS and BVS, this program is
 * designed to be used with OS redirection.
 *
 * NOTE: the constant K (maximum lag) MUST be smaller than the # of data
 * points in the text file, n.  Moreover, if the autocorrelations are to be
 * statistically meaningful, K should be MUCH smaller than n.
 *
 * Name              : Acs.java (AutoCorrelation Statistics)
 * Authors           : Steve Park & Dave Geyer  
 * Translation by    : Jun Wang
 * Language          : Java
 * Latest Revision   : 6-16-06
 * -------------------------------------------------------------------------
 */

package Libs;

import java.io.*;
import java.util.Scanner;

public class Acs {
  static int K    = 50;               /* K is the maximum lag          */
  static int SIZE = K + 1;

  public static void main(String[] args) {

    int i = 0;                   /* data point index              */
    int j;                       /* lag index                     */
    int p = 0;                   /* points to the head of 'hold'  */
    double x;                       /* current x[i] data point       */
    double sum = 0.0;               /* sums x[i]                     */
    long n;                       /* number of data points         */
    double mean;
    double hold[] = new double[SIZE]; /* K + 1 most recent data points */
    double cosum[] = new double[SIZE]; /* cosum[j] sums x[i] * x[i+j]   */

    for (j = 0; j < SIZE; j++)
      cosum[j] = 0.0;

    String fileName = "resources/results/infiniteHorizonStatsParcheggio.csv";
    File fileCalc = new File(fileName);

    try {                         /* the first K + 1 data values    */
      Scanner scanner = new Scanner(new FileInputStream(fileCalc));

      while (i < SIZE) { // initialize the hold array with
        //x = scanner.nextDouble(); // the first K + 1 data values
        String s = scanner.next();
        x = Double.parseDouble(s);
        sum += x;
        hold[(int) i] = x;
        i++;
      }

      while (scanner.hasNext()) {
        for (j = 0; j < SIZE; j++)
          cosum[(int) j] += hold[(int) p] * hold[(int) ((p + j) % SIZE)];
        String s = scanner.next();
        x = Double.parseDouble(s);
        sum += x;
        hold[(int) p] = x;
        p = (p + 1) % SIZE;
        i++;
      }
      n = i;

      while (i < n + SIZE) { // empty the circular array
        for (j = 0; j < SIZE; j++)
          cosum[(int) j] += hold[(int) p] * hold[(int) ((p + j) % SIZE)];
        hold[(int) p] = 0.0;
        p = (p + 1) % SIZE;
        i++;
      }

      mean = sum / n;
      for (j = 0; j <= K; j++)
        cosum[(int) j] = (cosum[(int) j] / (n - j)) - (mean * mean);

      System.out.println("for " + n + " data points");
      System.out.printf("the mean is ... %8.2f\n", mean);
      System.out.printf("the stdev is .. %8.2f\n\n", Math.sqrt(cosum[0]));
      System.out.println("  j (lag)   r[j] (autocorrelation)");
      for (j = 1; j < SIZE; j++)
        System.out.printf("%3d  %11.3f\n", j, cosum[(int) j] / cosum[0]);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }
}
