﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace KirinWP8
{
    abstract class KirinExtension
    {
        private string moduleName;

        protected KirinAssistant KirinAssistant { get; private set; }

        public KirinExtension(string moduleName, Kirin k)
        {
            this.moduleName = moduleName;
            KirinAssistant = k.BindScreen(this, moduleName);
            KirinAssistant.onLoad();
        }
    }
}