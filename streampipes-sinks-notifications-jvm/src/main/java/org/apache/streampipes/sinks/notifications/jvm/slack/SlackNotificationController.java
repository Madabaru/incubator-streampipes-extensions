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

package org.apache.streampipes.sinks.notifications.jvm.slack;

import com.ullink.slack.simpleslackapi.SlackChannel;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.SlackUser;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import org.apache.streampipes.model.DataSinkType;
import org.apache.streampipes.model.graph.DataSinkDescription;
import org.apache.streampipes.model.graph.DataSinkInvocation;
import org.apache.streampipes.sdk.builder.DataSinkBuilder;
import org.apache.streampipes.sdk.builder.StreamRequirementsBuilder;
import org.apache.streampipes.sdk.extractor.DataSinkParameterExtractor;
import org.apache.streampipes.sdk.helpers.EpRequirements;
import org.apache.streampipes.sdk.helpers.Labels;
import org.apache.streampipes.sdk.helpers.Locales;
import org.apache.streampipes.sdk.helpers.Options;
import org.apache.streampipes.sdk.utils.Assets;
import org.apache.streampipes.sinks.notifications.jvm.config.ConfigKeys;
import org.apache.streampipes.sinks.notifications.jvm.config.SinksNotificationsJvmConfig;
import org.apache.streampipes.wrapper.standalone.ConfiguredEventSink;
import org.apache.streampipes.wrapper.standalone.declarer.StandaloneEventSinkDeclarer;

import java.io.IOException;

public class SlackNotificationController extends StandaloneEventSinkDeclarer<SlackNotificationParameters> {

  private static final String CHANNEL_TYPE = "channel-type";
  private static final String RECEIVER = "receiver";
  private static final String CONTENT = "content";

  @Override
  public DataSinkDescription declareModel() {

    return DataSinkBuilder.create("org.apache.streampipes.sinks.notifications.jvm.slack")
            .withLocales(Locales.EN)
            .withAssets(Assets.DOCUMENTATION, Assets.ICON)
            .category(DataSinkType.NOTIFICATION)
            .requiredStream(StreamRequirementsBuilder
                    .create()
                    .requiredProperty(EpRequirements.anyProperty())
                    .build())
            .requiredTextParameter(Labels.withId(RECEIVER))
            .requiredTextParameter(Labels.withId(CONTENT))
            .requiredSingleValueSelection(Labels.withId(CHANNEL_TYPE),
                    Options.from("User", "Channel"))
            .build();
  }


  @Override
  public ConfiguredEventSink<SlackNotificationParameters> onInvocation(DataSinkInvocation graph, DataSinkParameterExtractor extractor) {
    String authToken = SinksNotificationsJvmConfig.INSTANCE.getSlackToken();

    if (authToken != ConfigKeys.SLACK_NOT_INITIALIZED) {
      // Initialize slack session
      SlackSession session = SlackSessionFactory.createWebSocketSlackSession(authToken);
      try {
        session.connect();
      } catch (IOException e) {
        e.printStackTrace();
        //throw new SpRuntimeException("Could not connect to Slack");
      }

      String userChannel = extractor.singleValueParameter(RECEIVER, String.class);
      String channelType = extractor.selectedSingleValue(CHANNEL_TYPE, String.class);
      String message = extractor.singleValueParameter(CONTENT, String.class);

      boolean sendToUser = channelType.equals("User");

      if (sendToUser) {
        SlackUser user = session.findUserByUserName(userChannel);
        if (user == null) {
          //throw new SpRuntimeException("The user: '" + userChannel + "' does not exists");
        }
      } else {
        SlackChannel channel = session.findChannelByName(userChannel);
        if (channel == null || channel.getId() == null) {
          //throw new SpRuntimeException("The channel: '" + userChannel + "' does not exists or " +
                  //"the bot has no rights to access it");
        }
      }

      SlackNotificationParameters params = new SlackNotificationParameters(graph, authToken,
              sendToUser, userChannel, message, session);

      return new ConfiguredEventSink<>(params, SlackNotification::new);

    } else {
      // TODO Exception handling of onInvocation
      //throw new SpRuntimeException("Slack not initialized");
      return null;
    }
  }
}