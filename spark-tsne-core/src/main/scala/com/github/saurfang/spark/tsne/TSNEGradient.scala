package com.github.saurfang.spark.tsne

import breeze.linalg._
import breeze.numerics._

object TSNEGradient {
  /**
   * Compute the TSNE Gradient at i. Update the gradient through dY then return costs attributed at i.
   *
   * @param data data point for row i by list of pair of (j, p_ij) and 0 <= j < n
   * @param i row number (0 <= i < n)
   * @param Y current Y [n * 2]
   * @param totalNum the common numerator that captures the t-distribution of Y
   * @param dY gradient of Y
   * @return loss attributed to row i
   */
  def compute(
               data: Iterable[(Int, Double)],
               i: Int,
               Y: DenseMatrix[Double],
               totalNum: Double,
               dY: DenseMatrix[Double],
               exaggeration: Boolean): Double = {
    val n = Y.rows
    val exaggeratedData = if(exaggeration) data.map{ case(j, v) => (j, v * 4) } else data
    val p = SparseVector(n)(exaggeratedData.toSeq: _*).toDenseVector

    // Y_diff = Y_i - Y
    val yDiff: DenseMatrix[Double] = (-Y).apply(*, ::) + Y(i, ::).inner
    // Y_sum = ||Y_i - Y||^2
    val sumY = sum((yDiff :* yDiff: DenseMatrix[Double])(*, ::))
    // num = (1 + ||Y_i - Y||^2)^-1
    val num = 1.0 :/ (1.0 :+ sumY)
    num(i) = 0
    // q = (1 + ||Y_i - Y_j||^2)^-1 / sum(1 + ||Y_k - Y_l||^2)^-1
    val q: DenseVector[Double] = num :/ totalNum
    // l = [ (p_ij - q_ij) * (1 + ||Y_i - Y_j||^2)^-1 ]
    val l = (p - q) :* num
    // l_sum = [0 0 ... sum(l) ... 0]
    val sumL = DenseVector.zeros[Double](n)
    sumL(i) = sum(l)
    // TODO: dY_i = 4 * (l_sum - l) * Y
    val dYi: DenseMatrix[Double] = 4.0 :* ((sumL - l).t * Y)
    dY(i, ::) := dYi.toDenseVector.t

    val logs = log(p :/ q).map(x => if (x.isNaN) 0.0 else x)
    sum(p :* logs)
  }
}