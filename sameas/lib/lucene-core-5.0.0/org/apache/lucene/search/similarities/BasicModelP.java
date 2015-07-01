package org.apache.lucene.search.similarities;

/*
 *
 * Copyright(c) 2015, Samsung Electronics Co., Ltd.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the <organization> nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.
    
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */ 

import static org.apache.lucene.search.similarities.SimilarityBase.log2;

/**
 * Implements the Poisson approximation for the binomial model for DFR.
 * @lucene.experimental
 * <p>
 * WARNING: for terms that do not meet the expected random distribution
 * (e.g. stopwords), this model may give poor performance, such as
 * abnormally high scores for low tf values.
 */
public class BasicModelP extends BasicModel {
  /** {@code log2(Math.E)}, precomputed. */
  protected static double LOG2_E = log2(Math.E);
  
  /** Sole constructor: parameter-free */
  public BasicModelP() {}
  
  @Override
  public final float score(BasicStats stats, float tfn) {
    float lambda = (float)(stats.getTotalTermFreq()+1) / (stats.getNumberOfDocuments()+1);
    return (float)(tfn * log2(tfn / lambda)
        + (lambda + 1 / (12 * tfn) - tfn) * LOG2_E
        + 0.5 * log2(2 * Math.PI * tfn));
  }

  @Override
  public String toString() {
    return "P";
  }
}