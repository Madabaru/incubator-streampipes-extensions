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
package org.apache.streampipes.processors.filters.jvm.processor.compose;

import org.apache.streampipes.model.constants.PropertySelectorConstants;
import org.apache.streampipes.model.runtime.Event;
import org.apache.streampipes.model.runtime.EventFactory;
import org.apache.streampipes.model.schema.EventSchema;
import org.apache.streampipes.wrapper.context.EventProcessorRuntimeContext;
import org.apache.streampipes.wrapper.routing.SpOutputCollector;
import org.apache.streampipes.wrapper.runtime.EventProcessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Compose implements EventProcessor<ComposeParameters> {

  private Map<String, Event> lastEvents;
  private EventSchema outputSchema;
  private List<String> outputKeySelectors;


  @Override
  public void onPipelineStarted(ComposeParameters composeParameters, SpOutputCollector spOutputCollector, EventProcessorRuntimeContext runtimeContext) {
    this.outputSchema = composeParameters.getGraph().getOutputStream().getEventSchema();
    this.outputKeySelectors = composeParameters.getOutputKeySelectors();
    this.lastEvents = new HashMap<>();
  }

  @Override
  public void onPipelineStopped() {
    this.lastEvents.clear();
  }

  @Override
  public void onEvent(Event event, SpOutputCollector spOutputCollector) {
    this.lastEvents.put(event.getSourceInfo().getSelectorPrefix(), event);
    if (lastEvents.size() == 2) {
      spOutputCollector.collect(buildOutEvent(event.getSourceInfo().getSelectorPrefix()));
    }
  }

  private Event buildOutEvent(String currentSelectorPrefix) {
    return EventFactory.fromEvents(lastEvents.get(currentSelectorPrefix), lastEvents.get
            (getOtherSelectorPrefix(currentSelectorPrefix)), outputSchema).getSubset(outputKeySelectors);
  }

  private String getOtherSelectorPrefix(String currentSelectorPrefix) {
    return currentSelectorPrefix.equals(PropertySelectorConstants.FIRST_STREAM_ID_PREFIX) ?
            PropertySelectorConstants.SECOND_STREAM_ID_PREFIX : PropertySelectorConstants
            .FIRST_STREAM_ID_PREFIX;
  }

}
