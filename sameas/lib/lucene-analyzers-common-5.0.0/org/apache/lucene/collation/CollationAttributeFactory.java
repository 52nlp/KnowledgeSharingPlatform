package org.apache.lucene.collation;

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

import java.text.Collator;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.collation.tokenattributes.CollatedTermAttributeImpl;
import org.apache.lucene.util.AttributeFactory;

/**
 * <p>
 *   Converts each token into its {@link java.text.CollationKey}, and then
 *   encodes the bytes as an index term.
 * </p>
 * <p>
 *   <strong>WARNING:</strong> Make sure you use exactly the same Collator at
 *   index and query time -- CollationKeys are only comparable when produced by
 *   the same Collator.  Since {@link java.text.RuleBasedCollator}s are not
 *   independently versioned, it is unsafe to search against stored
 *   CollationKeys unless the following are exactly the same (best practice is
 *   to store this information with the index and check that they remain the
 *   same at query time):
 * </p>
 * <ol>
 *   <li>JVM vendor</li>
 *   <li>JVM version, including patch version</li>
 *   <li>
 *     The language (and country and variant, if specified) of the Locale
 *     used when constructing the collator via
 *     {@link Collator#getInstance(java.util.Locale)}.
 *   </li>
 *   <li>
 *     The collation strength used - see {@link Collator#setStrength(int)}
 *   </li>
 * </ol> 
 * <p>
 *   The <code>ICUCollationAttributeFactory</code> in the analysis-icu package 
 *   uses ICU4J's Collator, which makes its
 *   version available, thus allowing collation to be versioned independently
 *   from the JVM.  ICUCollationAttributeFactory is also significantly faster and
 *   generates significantly shorter keys than CollationAttributeFactory.  See
 *   <a href="http://site.icu-project.org/charts/collation-icu4j-sun"
 *   >http://site.icu-project.org/charts/collation-icu4j-sun</a> for key
 *   generation timing and key length comparisons between ICU4J and
 *   java.text.Collator over several languages.
 * </p>
 * <p>
 *   CollationKeys generated by java.text.Collators are not compatible
 *   with those those generated by ICU Collators.  Specifically, if you use 
 *   CollationAttributeFactory to generate index terms, do not use
 *   ICUCollationAttributeFactory on the query side, or vice versa.
 * </p>
 */
public class CollationAttributeFactory extends AttributeFactory.StaticImplementationAttributeFactory<CollatedTermAttributeImpl> {
  private final Collator collator;
  
  /**
   * Create a CollationAttributeFactory, using 
   * {@link TokenStream#DEFAULT_TOKEN_ATTRIBUTE_FACTORY} as the
   * factory for all other attributes.
   * @param collator CollationKey generator
   */
  public CollationAttributeFactory(Collator collator) {
    this(TokenStream.DEFAULT_TOKEN_ATTRIBUTE_FACTORY, collator);
  }
  
  /**
   * Create a CollationAttributeFactory, using the supplied Attribute Factory 
   * as the factory for all other attributes.
   * @param delegate Attribute Factory
   * @param collator CollationKey generator
   */
  public CollationAttributeFactory(AttributeFactory delegate, Collator collator) {
    super(delegate, CollatedTermAttributeImpl.class);
    this.collator = collator;
  }
  
  @Override
  public CollatedTermAttributeImpl createInstance() {
    return new CollatedTermAttributeImpl(collator);
  }
}