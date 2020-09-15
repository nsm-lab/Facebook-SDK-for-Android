/*
 * Copyright (c) 2014-present, Facebook, Inc. All rights reserved.
 * <p>
 * You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 * copy, modify, and distribute this software in source code or binary form for use
 * in connection with the web services and APIs provided by Facebook.
 * <p>
 * As with any software that integrates with the Facebook platform, your use of
 * this software is subject to the Facebook Developer Principles and Policies
 * [http://developers.facebook.com/policy/]. This copyright notice shall be
 * included in all copies or substantial portions of the software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.internal.logging.monitor;

import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_SPENT;
import static com.facebook.internal.logging.monitor.MonitorLoggingTestUtil.TEST_TIME_START;
import static org.mockito.Mockito.when;

import com.facebook.FacebookPowerMockTestCase;
import com.facebook.FacebookSdk;
import com.facebook.internal.logging.ExternalLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.Executor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.reflect.Whitebox;
import org.robolectric.RuntimeEnvironment;

@PrepareForTest({FacebookSdk.class})
public class MonitorLoggingStoreTest extends FacebookPowerMockTestCase {

  private final Executor mockExecutor = new FacebookSerialExecutor();
  private static final int LOGS_BATCH_NUMBER = 3;

  @Before
  public void init() {
    PowerMockito.spy(FacebookSdk.class);
    when(FacebookSdk.isInitialized()).thenReturn(true);
    Whitebox.setInternalState(FacebookSdk.class, "executor", mockExecutor);
    PowerMockito.when(FacebookSdk.getApplicationContext())
        .thenReturn(RuntimeEnvironment.application);
  }

  @Test
  public void testWriteAndReadFromStore() {
    MonitorLoggingStore logStore = MonitorLoggingStore.getInstance();

    Collection<ExternalLog> firstLogsWriteToStore = new ArrayList<>();
    MonitorLog log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START);
    firstLogsWriteToStore.add(log);
    logStore.saveLogsToDisk(firstLogsWriteToStore);

    // add logs after adding the first log list
    // to see if the first ones are erased
    Collection<ExternalLog> secondLogsWriteToStore = new ArrayList<>();
    for (int i = 0; i < LOGS_BATCH_NUMBER; i++) {
      log = MonitorLoggingTestUtil.getTestMonitorLog(TEST_TIME_START, TEST_TIME_SPENT);
      secondLogsWriteToStore.add(log);
    }

    logStore.saveLogsToDisk(secondLogsWriteToStore);
    Collection<ExternalLog> logsReadFromStore = logStore.readAndClearStore();

    // Logs read from store should only have the ones we added at second time
    // compare the size
    Assert.assertEquals(secondLogsWriteToStore.size(), logsReadFromStore.size());

    Iterator<ExternalLog> iteratorOfSecondLogsWriteToStore = secondLogsWriteToStore.iterator();
    Iterator<ExternalLog> iteratorOfLogsReadFromStore = logsReadFromStore.iterator();

    while (iteratorOfSecondLogsWriteToStore.hasNext() && iteratorOfLogsReadFromStore.hasNext()) {
      Assert.assertEquals(
          iteratorOfSecondLogsWriteToStore.next(), iteratorOfLogsReadFromStore.next());
    }

    // make sure the file is deleted
    logsReadFromStore = logStore.readAndClearStore();
    Assert.assertEquals(0, logsReadFromStore.size());
  }
}
