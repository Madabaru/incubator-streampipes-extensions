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
package org.apache.streampipes.processors.geo.jvm.processor.revgeocoder;

import org.apache.streampipes.commons.exceptions.SpRuntimeException;
import org.apache.streampipes.model.runtime.Event;
import org.apache.streampipes.processors.geo.jvm.processor.revgeocoder.geocode.GeoName;
import org.apache.streampipes.processors.geo.jvm.processor.revgeocoder.geocode.ReverseGeoCode;
import org.apache.streampipes.wrapper.context.EventProcessorRuntimeContext;
import org.apache.streampipes.wrapper.routing.SpOutputCollector;
import org.apache.streampipes.wrapper.runtime.EventProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class ReverseGeocoding implements EventProcessor<ReverseGeocodingParameters> {

  private String latitudeField;
  private String longitudeField;

  private ReverseGeoCode reverseGeoCode;

  @Override
  public void onPipelineStarted(ReverseGeocodingParameters parameters, SpOutputCollector spOutputCollector, EventProcessorRuntimeContext runtimeContext) throws SpRuntimeException {
    this.latitudeField = parameters.getLatitudeField();
    this.longitudeField = parameters.getLongitudeField();

    InputStream stream = getClass()
            .getClassLoader().getResourceAsStream("cities1000.zip");
    if (stream != null) {
      ZipInputStream zipInputStream = null;
      zipInputStream = new ZipInputStream(stream);
      try {
        this.reverseGeoCode = new ReverseGeoCode(zipInputStream, false);
      } catch (IOException e) {
        throw new SpRuntimeException("Could not load cities file...");
      }
    }
  }

  @Override
  public void onEvent(Event event, SpOutputCollector collector) throws SpRuntimeException {
    Double latitude = event.getFieldBySelector(latitudeField).getAsPrimitive().getAsDouble();
    Double longitude = event.getFieldBySelector(longitudeField).getAsPrimitive().getAsDouble();

    GeoName geoName = this.reverseGeoCode.nearestPlace(latitude, longitude);

    event.addField("place", geoName.name + ", " + geoName.country);
    collector.collect(event);
  }

  @Override
  public void onPipelineStopped() throws SpRuntimeException {

  }
}
