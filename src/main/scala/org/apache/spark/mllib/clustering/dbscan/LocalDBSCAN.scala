/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.mllib.clustering.dbscan

import scala.collection.mutable.ListBuffer
import org.apache.spark.Logging

class LocalDBSCAN(eps: Double, minPoints: Int) extends Logging {

  val minDistanceSquared = eps * eps

  def fit(vectors: List[LabeledVector]): List[LabeledVector] = {
    
    logInfo("About to start fitting")

    var cluster = Unlabeled

    vectors.foreach(vector => {

      if (!vector.visited) {
        vector.visited = true

        val neighbors = findNeighbors(vector, vectors)

        if (neighbors.length < minPoints) {
          vector.label = Noise
        } else {
          cluster += 1
          expandCluster(vector, neighbors, vectors, cluster)
        }

      }
      logDebug(s"iterating cluster:$cluster")

    })

    logInfo("done...")

    vectors

  }

  def findNeighbors(vector: LabeledVector, all: List[LabeledVector]): List[LabeledVector] =
    all.filter(other => distanceSquared(vector, other) <= minDistanceSquared)

  def distanceSquared(vector: LabeledVector, other: LabeledVector): Double =
    vector.point.distanceSquared(other.point)

  def expandCluster(
    vector: LabeledVector,
    inneighbors: List[LabeledVector],
    all: List[LabeledVector],
    cluster: Int): Unit = {

    vector.isCore = true
    vector.label = cluster

    val neighbors = inneighbors.to[ListBuffer]

    neighbors.foreach(neighbor => {

      if (!neighbor.visited) {

        neighbor.visited = true

        val curNeighbors = findNeighbors(neighbor, all)

        if (curNeighbors.length >= minPoints) {
          neighbors ++= curNeighbors
          neighbor.isCore = true
        } else {
          neighbor.isBorder = true
        }

      }

      if (neighbor.label == Unlabeled) {
        neighbor.label = cluster
      }

    })

  }
}
