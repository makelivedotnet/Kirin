﻿using Microsoft.Phone.Controls;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Runtime.Serialization.Json;
using System.Text;
using System.Threading.Tasks;  
using System.Windows.Threading;

namespace KirinWP8
{
    public class KirinWebViewHolder
    {
        private WebBrowser wb;
        private NativeContext context;
        private bool navigated = false;
        private Queue<string> queue = new Queue<string>();

        public KirinWebViewHolder(NativeContext context)
        {
            this.context = context;
            wb = new WebBrowser();
            wb.IsScriptEnabled = true;
            wb.ScriptNotify += wb_ScriptNotify;
            wb.Navigated += wb_Navigated;
            wb.Navigate(new Uri("/generated-javascript/index-wp8.html", UriKind.Relative));
        }

        void wb_Navigated(object sender, System.Windows.Navigation.NavigationEventArgs e)
        {
            navigated = true;
            while (queue.Count > 0)
            {
                string script = queue.Dequeue();
                ActuallyInvokeIt(script);
            }
        }

        private void ActuallyInvokeIt(string script)
        {
            string fn = "(function() { try {";
            fn += script + ";";
            fn += "} catch (e) { window.external.notify(e); } })();";
            Debug.WriteLine("actually invoking " + fn);

            wb.Dispatcher.BeginInvoke(() =>
            {
                try
                {
                    wb.InvokeScript("eval", fn);
                }
                catch (Exception ex)
                {
                    Debug.WriteLine(ex.Message);
                }
            }); 
        }

        public void InvokeScriptoids(string script)
        {
            if (!navigated) { queue.Enqueue(script); }
            else
            {
                ActuallyInvokeIt(script);
            }   
        }

        void wb_ScriptNotify(object sender, NotifyEventArgs e)
        {
            if (!e.Value.StartsWith("native"))
            {
                Debug.WriteLine(e.Value);
                return;
            }
            Uri uri = new Uri(e.Value);
            if ("native".Equals(uri.Scheme))
            {
                // Here's an example URI:
                // native://DebugConsole.log_atLevel_/?%5B%22Javascript%20says%3A%20windaes%20webview%20loaded%22%2C%22INFO%22%5D

                // We need to parse the URI ourselves because uri.host loses string case information
                string schemelessUri = e.Value.Substring("native://".Length);

                // ok our string now looks like this: 
                // DebugConsole.log_atLevel_/?%5B%22Javascript%20says%3A%20windaes%20webview%20loaded%22%2C%22INFO%22%5D
                // everything before /? is class and method, everything after /? is the parameters
                int slash = schemelessUri.IndexOf('/');
                string classAndMethod = schemelessUri.Substring(0, slash);
                string parameters = schemelessUri.Substring(slash + 2);

                // before the . is the classname, after the . is the method
                int dot = classAndMethod.IndexOf('.');
                string className = classAndMethod.Substring(0, dot);
                string method = classAndMethod.Substring(dot + 1);

                context.PerformMethod(className, method, parameters);
                wb.InvokeScript("eval", "EXPOSED_TO_NATIVE.js_ObjC_bridge.ready = true;");
            }
        }

        private void debugUri(Uri uri)
        {
            Debug.WriteLine("AbsolutePath:   " + uri.AbsolutePath);
            Debug.WriteLine("AbsoluteUri:    " + uri.AbsoluteUri);
            Debug.WriteLine("Authority:      " + uri.Authority);
            Debug.WriteLine("DnsSafeHost:    " + uri.DnsSafeHost);
            Debug.WriteLine("Host:           " + uri.Host);
            Debug.WriteLine("HostNameType:   " + uri.HostNameType);
            Debug.WriteLine("IsAbsoluteUri:  " + uri.IsAbsoluteUri);
            Debug.WriteLine("IsDefaultPort:  " + uri.IsDefaultPort);
            Debug.WriteLine("IsFile:         " + uri.IsFile);
            Debug.WriteLine("IsLoopback:     " + uri.IsLoopback);
            Debug.WriteLine("IsUnc:          " + uri.IsUnc);
            Debug.WriteLine("LocalPath:      " + uri.LocalPath);
            Debug.WriteLine("OriginalString: " + uri.OriginalString);
            Debug.WriteLine("PathAndQuery:   " + uri.PathAndQuery);
            Debug.WriteLine("Port:           " + uri.Port);
            Debug.WriteLine("Query:          " + uri.Query);
            Debug.WriteLine("Scheme:         " + uri.Scheme);
            Debug.WriteLine("Segments:       " + uri.Segments);
            Debug.WriteLine("UserEscaped:    " + uri.UserEscaped);
            Debug.WriteLine("UserInfo:       " + uri.UserInfo);
        }
    }
}