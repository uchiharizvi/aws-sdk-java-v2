/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.utils.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.utils.async.ByteBufferStoringSubscriber.TransferResult;

public class ByteBufferStoringSubscriberTest {
    @Test
    public void constructorCalled_withNonPositiveSize_throwsException() {
        assertThatCode(() -> new ByteBufferStoringSubscriber(1)).doesNotThrowAnyException();
        assertThatCode(() -> new ByteBufferStoringSubscriber(Integer.MAX_VALUE)).doesNotThrowAnyException();

        assertThatThrownBy(() -> new ByteBufferStoringSubscriber(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ByteBufferStoringSubscriber(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ByteBufferStoringSubscriber(Integer.MIN_VALUE)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void doesNotRequestMoreThanMaxBytes() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(3);
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        verify(subscription).request(1);

        subscriber.onNext(fullByteBufferOfSize(2));
        verify(subscription, times(2)).request(1);

        subscriber.onNext(fullByteBufferOfSize(0));
        verify(subscription, times(3)).request(1);

        subscriber.onNext(fullByteBufferOfSize(1));
        verifyNoMoreInteractions(subscription);
    }

    @Test
    public void canStoreMoreThanMaxBytesButWontAskForMoreUntilBelowMax() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(3);
        Subscription subscription = mock(Subscription.class);

        subscriber.onSubscribe(subscription);
        verify(subscription).request(1);

        subscriber.onNext(fullByteBufferOfSize(1)); // After: Storing 1
        verify(subscription, times(2)).request(1); // It should request more

        subscriber.onNext(fullByteBufferOfSize(50)); // After: Storing 51
        subscriber.transferTo(emptyByteBufferOfSize(48)); // After: Storing 3
        verifyNoMoreInteractions(subscription); // It should NOT request more

        subscriber.transferTo(emptyByteBufferOfSize(1)); // After: Storing 2
        verify(subscription, times(3)).request(1); // It should request more
    }

    @Test
    public void noDataTransferredIfNoDataBuffered() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));

        ByteBuffer out = emptyByteBufferOfSize(1);

        assertThat(subscriber.transferTo(out)).isEqualTo(TransferResult.SUCCESS);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void noDataTransferredIfComplete() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onComplete();

        ByteBuffer out = emptyByteBufferOfSize(1);

        assertThat(subscriber.transferTo(out)).isEqualTo(TransferResult.END_OF_STREAM);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void noDataTransferredIfError() {
        RuntimeException error = new RuntimeException();

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onError(error);

        ByteBuffer out = emptyByteBufferOfSize(1);

        assertThatThrownBy(() -> subscriber.transferTo(out)).isEqualTo(error);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void checkedExceptionsAreWrapped() {
        Exception error = new Exception();

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onError(error);

        ByteBuffer out = emptyByteBufferOfSize(1);

        assertThatThrownBy(() -> subscriber.transferTo(out)).hasCause(error);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void completeIsReportedEvenWithExactOutSize() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(fullByteBufferOfSize(2));
        subscriber.onComplete();

        ByteBuffer out = emptyByteBufferOfSize(2);
        assertThat(subscriber.transferTo(out)).isEqualTo(TransferResult.END_OF_STREAM);
        assertThat(out.remaining()).isEqualTo(0);
    }

    @Test
    public void completeIsReportedEvenWithExtraOutSize() {
        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(fullByteBufferOfSize(2));
        subscriber.onComplete();

        ByteBuffer out = emptyByteBufferOfSize(3);
        assertThat(subscriber.transferTo(out)).isEqualTo(TransferResult.END_OF_STREAM);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void errorIsReportedEvenWithExactOutSize() {
        RuntimeException error = new RuntimeException();

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(fullByteBufferOfSize(2));
        subscriber.onError(error);

        ByteBuffer out = emptyByteBufferOfSize(2);
        assertThatThrownBy(() -> subscriber.transferTo(out)).isEqualTo(error);
        assertThat(out.remaining()).isEqualTo(0);
    }

    @Test
    public void errorIsReportedEvenWithExtraOutSize() {
        RuntimeException error = new RuntimeException();

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(2);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(fullByteBufferOfSize(2));
        subscriber.onError(error);

        ByteBuffer out = emptyByteBufferOfSize(3);
        assertThatThrownBy(() -> subscriber.transferTo(out)).isEqualTo(error);
        assertThat(out.remaining()).isEqualTo(1);
    }

    @Test
    public void dataIsDeliveredInTheRightOrder() {
        ByteBuffer buffer1 = fullByteBufferOfSize(1);
        ByteBuffer buffer2 = fullByteBufferOfSize(1);
        ByteBuffer buffer3 = fullByteBufferOfSize(1);

        ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(3);
        subscriber.onSubscribe(mock(Subscription.class));
        subscriber.onNext(buffer1);
        subscriber.onNext(buffer2);
        subscriber.onNext(buffer3);
        subscriber.onComplete();

        ByteBuffer out = emptyByteBufferOfSize(4);
        subscriber.transferTo(out);

        out.flip();
        assertThat(out.get()).isEqualTo(buffer1.get());
        assertThat(out.get()).isEqualTo(buffer2.get());
        assertThat(out.get()).isEqualTo(buffer3.get());
        assertThat(out.hasRemaining()).isFalse();
    }

    @Test
    @Timeout(30)
    public void stochastic_subscriberSeemsThreadSafe() throws Throwable {
        ExecutorService producer = Executors.newFixedThreadPool(1);
        ExecutorService consumer = Executors.newFixedThreadPool(1);
        try {
            ByteBufferStoringSubscriber subscriber = new ByteBufferStoringSubscriber(50);

            AtomicBoolean testRunning = new AtomicBoolean(true);
            AtomicInteger messageNumber = new AtomicInteger(0);

            AtomicReference<Throwable> producerFailure = new AtomicReference<>();
            Subscription subscription = new Subscription() {
                @Override
                public void request(long n) {
                    producer.submit(() -> {
                        try {
                            for (int i = 0; i < n; i++) {
                                ByteBuffer buffer = ByteBuffer.allocate(4);
                                buffer.putInt(messageNumber.getAndIncrement());
                                buffer.flip();
                                subscriber.onNext(buffer);
                            }
                        } catch (Throwable t) {
                            producerFailure.set(t);
                        }
                    });
                }

                @Override
                public void cancel() {
                    producerFailure.set(new AssertionError("Cancel not expected."));
                }
            };

            subscriber.onSubscribe(subscription);

            Future<Object> consumerFuture = consumer.submit(() -> {
                ByteBuffer carryOver = ByteBuffer.allocate(4);

                int expectedMessageNumber = 0;
                while (testRunning.get()) {
                    Thread.sleep(1);

                    ByteBuffer out = ByteBuffer.allocate(4 + expectedMessageNumber);
                    subscriber.transferTo(out);

                    out.flip();

                    if (carryOver.position() > 0) {
                        int oldOutLimit = out.limit();
                        out.limit(carryOver.remaining());
                        carryOver.put(out);
                        out.limit(oldOutLimit);

                        carryOver.flip();
                        assertThat(carryOver.getInt()).isEqualTo(expectedMessageNumber);
                        ++expectedMessageNumber;
                        carryOver.clear();
                    }

                    while (out.remaining() >= 4) {
                        assertThat(out.getInt()).isEqualTo(expectedMessageNumber);
                        ++expectedMessageNumber;
                    }

                    if (out.hasRemaining()) {
                        carryOver.put(out);
                    }
                }
                return null;
            });

            Thread.sleep(5_000);
            testRunning.set(false);
            consumerFuture.get();
            if (producerFailure.get() != null) {
                throw producerFailure.get();
            }
            assertThat(messageNumber.get()).isGreaterThan(10); // ensure we actually tested something
        } finally {
            producer.shutdownNow();
            consumer.shutdownNow();
        }
    }

    private ByteBuffer fullByteBufferOfSize(int size) {
        byte[] data = new byte[size];
        ThreadLocalRandom.current().nextBytes(data);
        return ByteBuffer.wrap(data);
    }

    private ByteBuffer emptyByteBufferOfSize(int size) {
        return ByteBuffer.allocate(size);
    }
}