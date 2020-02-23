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
 *
 */

package org.apache.streampipes.processors.textmining.jvm.processor.tokenizer;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.streampipes.logging.api.Logger;
import org.apache.streampipes.model.runtime.Event;
import org.apache.streampipes.wrapper.context.EventProcessorRuntimeContext;
import org.apache.streampipes.wrapper.routing.SpOutputCollector;
import org.apache.streampipes.wrapper.runtime.EventProcessor;

import java.io.IOException;
import java.io.InputStream;

public class Tokenizer implements EventProcessor<TokenizerParameters> {

  private static Logger LOG;

  // Field with the text
  private String detection;
  private TokenizerME tokenizer;

  public Tokenizer() {
    try (InputStream modelIn = getClass().getClassLoader().getResourceAsStream("tokenizer-en.bin")) {
      TokenizerModel model = new TokenizerModel(modelIn);
      tokenizer = new TokenizerME(model);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void onPipelineStarted(TokenizerParameters tokenizerParameters,
                                SpOutputCollector spOutputCollector,
                                EventProcessorRuntimeContext runtimeContext) {
    LOG = tokenizerParameters.getGraph().getLogger(Tokenizer.class);
    this.detection = tokenizerParameters.getDetectionName();
  }

  @Override
  public void onEvent(Event inputEvent, SpOutputCollector out) {
    String text = inputEvent.getFieldBySelector(detection).getAsPrimitive().getAsString();

    inputEvent.addField(TokenizerController.TOKEN_LIST_FIELD_KEY, tokenizer.tokenize(text));

    out.collect(inputEvent);
  }

  @Override
  public void onPipelineStopped() {
  }
}
