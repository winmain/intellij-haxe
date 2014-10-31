/*
 * Copyright 2000-2013 JetBrains s.r.o.
 * Copyright 2014-2014 AS3Boyan
 * Copyright 2014-2014 Elias Ku
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.plugins.haxe.haxelib;

import com.google.common.base.Joiner;

import java.util.ArrayList;

/**
 * Created by as3boyan on 31.10.14.
 */
public class HaxelibParser {
  public static String stringifyHaxelib(String name, String version) {
    ArrayList<String> strings = new ArrayList<String>();
    strings.add("haxelib");
    strings.add(name);
    strings.add(version);
    return Joiner.on("|").join(strings);
  }

  public static HaxelibItem parseHaxelib(String data) {
    String[] strings = data.split("|");

    if (strings.length == 3 && strings[0].equals("haxelib")) {
      return new HaxelibItem(strings[1], strings[2]);
    }

    return null;
  }
}