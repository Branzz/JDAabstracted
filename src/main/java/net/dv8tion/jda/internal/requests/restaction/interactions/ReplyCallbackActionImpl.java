/*
 * Copyright 2015 Austin Keener, Michael Ritter, Florian Spieß, and the JDA contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.dv8tion.jda.internal.requests.restaction.interactions;

import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import net.dv8tion.jda.api.utils.AttachedFile;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.data.DataObject;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.dv8tion.jda.internal.interactions.InteractionHookImpl;
import net.dv8tion.jda.internal.utils.message.MessageCreateBuilderMixin;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

public class ReplyCallbackActionImpl extends DeferrableCallbackActionImpl implements ReplyCallbackAction, MessageCreateBuilderMixin<ReplyCallbackAction>
{
    private final MessageCreateBuilder builder = new MessageCreateBuilder();
    private int flags;

    public ReplyCallbackActionImpl(InteractionHookImpl hook)
    {
        super(hook);
    }

    @Override
    public MessageCreateBuilder getBuilder()
    {
        return builder;
    }

    @Nonnull
    @Override
    public ReplyCallbackActionImpl closeResources()
    {
        builder.closeFiles();
        return this;
    }

    @Nonnull
    protected RequestBody finalizeData()
    {
        DataObject json = DataObject.empty();
        if (isEmpty())
            return getRequestBody(json.put("type", ResponseType.DEFERRED_CHANNEL_MESSAGE_WITH_SOURCE.getRaw()));
        json.put("type", ResponseType.CHANNEL_MESSAGE_WITH_SOURCE.getRaw());
        MessageCreateData data = builder.build();
        try
        {
            json.put("data", data.toData().put("flags", flags));
            List<FileUpload> files = data.getFiles();
            if (files.isEmpty())
                return getRequestBody(json);

            MultipartBody.Builder body = AttachedFile.createMultipartBody(files, null);
            body.addFormDataPart("payload_json", json.toString());
            return body.build();
        }
        catch (Exception e)
        {
            data.close();
            throw e;
        }
    }

    private boolean isEmpty()
    {
        //Intentionally does not check components.isEmpty() here
        // You cannot send a message with only components at this time.
        return builder.isEmpty();
    }

    @Nonnull
    @Override
    public ReplyCallbackActionImpl setEphemeral(boolean ephemeral)
    {
        if (ephemeral)
            this.flags |= 64;
        else
            this.flags &= ~64;
        return this;
    }

    @Nonnull
    @Override
    public ReplyCallbackAction setCheck(BooleanSupplier checks)
    {
        return (ReplyCallbackAction) super.setCheck(checks);
    }

    @Nonnull
    @Override
    public ReplyCallbackAction timeout(long timeout, @Nonnull TimeUnit unit)
    {
        return (ReplyCallbackAction) super.timeout(timeout, unit);
    }

    @Nonnull
    @Override
    public ReplyCallbackAction deadline(long timestamp)
    {
        return (ReplyCallbackAction) super.deadline(timestamp);
    }
}
