/*
 * KeyControl
 * Connect SDK
 *
 * Copyright (c) 2014 LG Electronics.
 * Created by Hyun Kook Khang on 19 Jan 2014
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.connectsdk.service.capability;

import com.connectsdk.service.capability.listeners.ResponseListener;

public interface KeyControl extends CapabilityMethods {
    String Any = "KeyControl.Any";

    String Up = "KeyControl.Up";
    String Down = "KeyControl.Down";
    String Left = "KeyControl.Left";
    String Right = "KeyControl.Right";
    String OK = "KeyControl.OK";
    String Back = "KeyControl.Back";
    String Home = "KeyControl.Home";
    String Send_Key = "KeyControl.SendKey";
    String KeyCode = "KeyControl.KeyCode";
    String[] Capabilities = {
            Up,
            Down,
            Left,
            Right,
            OK,
            Back,
            Home,
            KeyCode,
    };

    KeyControl getKeyControl();

    CapabilityPriorityLevel getKeyControlCapabilityLevel();

    void up(ResponseListener<Object> listener);

    void down(ResponseListener<Object> listener);

    void left(ResponseListener<Object> listener);

    void right(ResponseListener<Object> listener);

    void ok(ResponseListener<Object> listener);

    void back(ResponseListener<Object> listener);

    void exitLG(ResponseListener<Object> listener);

    void home(ResponseListener<Object> listener);

    void power(ResponseListener<Object> responseListener);

    void guideLG(ResponseListener<Object> responseListener);

    void settingLG(ResponseListener<Object> responseListener);

    void sendKeyCode(mNumKeyCode keycode, ResponseListener<Object> listener);

    void red(ResponseListener<Object> responseListener);

    void green(ResponseListener<Object> responseListener);

    void yellow(ResponseListener<Object> responseListener);

    void blue(ResponseListener<Object> responseListener);

    void voiceLg(ResponseListener<Object> responseListener);

    enum mNumKeyCode {
        NUM_0(0),
        NUM_1(1),
        NUM_2(2),
        NUM_3(3),
        NUM_4(4),
        NUM_5(5),
        NUM_6(6),
        NUM_7(7),
        NUM_8(8),
        NUM_9(9),

        DASH(10),
        ENTER(11);

        private static final mNumKeyCode[] codes = {
                NUM_0, NUM_1, NUM_2, NUM_3, NUM_4, NUM_5, NUM_6, NUM_7, NUM_8, NUM_9, DASH, ENTER
        };
        private final int code;

        mNumKeyCode(int code) {
            this.code = code;
        }

        public static mNumKeyCode createFromInteger(int keyCode) {
            if (keyCode >= 0 && keyCode < codes.length) {
                return codes[keyCode];
            }
            return null;
        }

        public int getCode() {
            return code;
        }
    }
}
