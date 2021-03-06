package com.onehilltech.concurrent;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.Executors;

public class WaterfallTest
{
  private boolean callbackCalled_;

  @Before
  public void setup ()
  {
    this.callbackCalled_ = false;
  }

  @Test
  public void testExecute () throws Exception
  {
    final Waterfall waterfall = new Waterfall (
        Executors.newCachedThreadPool (),
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            Assert.assertNull (lastResult);
            System.err.println ("Running task one...");

            callback.done (1);
          }
        },
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            Assert.assertEquals (1, lastResult);
            System.err.println ("Running task two...");

            callback.done (2);
          }
        });

    synchronized (waterfall)
    {
      Future future = waterfall.execute (new CompletionCallback <Object> ()
      {
        @Override
        public void onComplete (Object result)
        {
          System.err.println ("Task completed...");

          Assert.assertEquals (2, result);
          callbackCalled_ = true;

          synchronized (waterfall)
          {
            waterfall.notify ();
          }
        }

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
      });

      if (!future.isDone ())
        waterfall.wait (5000);

      Assert.assertEquals (true, this.callbackCalled_);
    }
  }

  @Test
  public void testExecuteFail () throws Exception
  {
    final Waterfall waterfall = new Waterfall (
        Executors.newCachedThreadPool (),
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            callback.done (1);
          }
        },
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            callback.fail (new Exception ("IDK"));
          }
        });

    synchronized (waterfall)
    {
      Future future = waterfall.execute (new CompletionCallback <Object> ()
      {
        @Override
        public void onComplete (Object result)
        {
          Assert.fail ();
        }

        @Override
        public void onFail (Throwable e)
        {
          Assert.assertEquals (e.getMessage (), "IDK");
          callbackCalled_ = true;

          synchronized (waterfall)
          {
            waterfall.notify ();
          }
        }

        @Override
        public void onCancel ()
        {
          Assert.fail ();
        }
      });

      if (!future.isDone ())
        waterfall.wait (5000);

      Assert.assertEquals (true, this.callbackCalled_);
    }
  }

  @Test
  public void testExecuteCancel () throws Exception
  {
    final Waterfall waterfall = new Waterfall (
        Executors.newCachedThreadPool (),
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            try
            {
              Thread.sleep (1000);
              callback.done (null);
            }
            catch (InterruptedException e)
            {
              throw new RuntimeException (e);
            }
          }
        },
        new Task () {
          @Override
          public void run (Object lastResult, CompletionCallback callback)
          {
            callback.done (lastResult);
          }
        });

    synchronized (waterfall)
    {
      Future future = waterfall.execute (new CompletionCallback <Object> ()
      {
        @Override
        public void onComplete (Object result)
        {
          Assert.fail ();
        }

        @Override
        public void onFail (Throwable e)
        {
          Assert.fail ();
        }

        @Override
        public void onCancel ()
        {
          callbackCalled_ = true;

          synchronized (waterfall)
          {
            waterfall.notify ();
          }
        }
      });

      // Cancel the waterfall, and wait until notification.
      future.cancel ();
      waterfall.wait (5000);

      // Make sure the cancel callback is called.
      Assert.assertEquals (true, this.callbackCalled_);
    }
  }
}
