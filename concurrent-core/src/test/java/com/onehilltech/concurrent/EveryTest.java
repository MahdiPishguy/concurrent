package com.onehilltech.concurrent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.Executors;

public class EveryTest
{
  private boolean callbackCalled_;

  @Before
  public void setup ()
  {
    this.callbackCalled_ = false;
  }

  @Test
  public void testExecuteAndFound () throws Exception
  {
    Integer[] nums = {3, 3, 3, 3};

    Future future =
        new Every<> (Executors.newCachedThreadPool (),
                       new Task<Integer> ()
                       {
                         @Override
                         public void run (Integer item, CompletionCallback callback)
                         {
                           callback.done (item == 3);
                         }
                       }).execute (Arrays.asList (nums), new CompletionCallback <Boolean> ()
        {
          @Override
          public void onFail (Throwable e)
          {
            Assert.fail ();
          }

          @Override
          public void onCancel ()
          {
            Assert.fail ();
          }

          @Override
          public void onComplete (Boolean result)
          {
            Assert.assertEquals (true, result);
            callbackCalled_ = true;

            synchronized (EveryTest.this)
            {
              EveryTest.this.notify ();
            }
          }
        });

    synchronized (this)
    {
      this.wait ();

      Assert.assertTrue (future.isDone ());
      Assert.assertTrue (this.callbackCalled_);
    }
  }

  @Test
  public void testExecuteAndNotFound () throws Exception
  {
    final Integer[] nums = {3, 3, 3, 4};

    Future future =
        new Every<> (Executors.newCachedThreadPool (),
                     new Task<Integer> ()
                     {
                       @Override
                       public void run (Integer item, CompletionCallback callback)
                       {
                         callback.done (item == 3);
                       }
                     }).execute (Arrays.asList (nums), new CompletionCallback<Boolean> ()
        {
          @Override
          public void onFail (Throwable e)
          {
            Assert.fail ();
          }

          @Override
          public void onCancel ()
          {
            Assert.fail ();
          }

          @Override
          public void onComplete (Boolean result)
          {
            Assert.assertEquals (false, result);
            callbackCalled_ = true;

            synchronized (EveryTest.this)
            {
              EveryTest.this.notify ();
            }
          }
        });

    synchronized (this)
    {
      this.wait ();

      Assert.assertTrue (future.isDone ());
      Assert.assertTrue (this.callbackCalled_);
    }
  }
}
