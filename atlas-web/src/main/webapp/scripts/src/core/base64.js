/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

(function(A) {
    A.Base64 = {
        encode: function(str) {
            return base64Encode(str);
        },

        decode: function(str) {
            return base64Decode(str);
        }
    };

    var b64array = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    function base64Encode(text) {
        text = utf8Decode(text);

        var out = [];
        var hex = "";

        var c1, c2, c3, enc1, enc2, enc3, enc4;
        var i = 0, len = text.length;

        while (i < len) {
            c1 = c2 = c3 = "";
            enc1 = enc2 = enc3 = enc4 = "";

            c1 = text.charCodeAt(i++);
            c2 = text.charCodeAt(i++);
            c3 = text.charCodeAt(i++);

            enc1 = c1 >> 2;
            enc2 = ((c1 & 3) << 4) | (c2 >> 4);
            enc3 = ((c2 & 15) << 2) | (c3 >> 6);
            enc4 = c3 & 63;

            if (isNaN(c2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(c3)) {
                enc4 = 64;
            }

            out.push(
                b64array.charAt(enc1) +
                    b64array.charAt(enc2) +
                    b64array.charAt(enc3) +
                    b64array.charAt(enc4));

        }

        return out.join("");
    }

    function utf8Decode(text) {
        var c, c2, c3, out = [];
        var i = 0, len = text.length;

        while (i < len) {
            c = text.charCodeAt(i);

            if (c < 128) {
                out.push(String.fromCharCode(c));
                i++;
            } else if ((c > 191) && (c < 224)) {
                c2 = text.charCodeAt(i + 1);
                out.push(String.fromCharCode(((c & 31) << 6) | (c2 & 63)));
                i += 2;
            } else {
                c2 = text.charCodeAt(i + 1);
                c3 = text.charCodeAt(i + 2);
                out.push(String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63)));
                i += 3;
            }
        }
        return out.join("");
    }

    function base64Decode(text) {
        var invalidChars = /[^A-Za-z0-9\+\/\=]/g;
        if (invalidChars.exec(text)) {
            A.logError("base64 decode - Invalid base64 characters: " + text);
            text = text.replace(invalidChars, "");
        }

        var out = [];

        var c1, c2, c3, enc1, enc2, enc3, enc4;
        var i = 0, len = text.length;

        while (i < len) {
            c1 = c2 = c3 = "";
            enc1 = enc2 = enc3 = enc4 = "";

            enc1 = b64array.indexOf(input.charAt(i++));
            enc2 = b64array.indexOf(input.charAt(i++));
            enc3 = b64array.indexOf(input.charAt(i++));
            enc4 = b64array.indexOf(input.charAt(i++));

            c1 = (enc1 << 2) | (enc2 >> 4);
            c2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            c3 = ((enc3 & 3) << 6) | enc4;

            out.push(String.fromCharCode(c1));

            if (enc3 != 64) {
                out.push(String.fromCharCode(c2));
            }

            if (enc4 != 64) {
                out.push(String.fromCharCode(c3));
            }

        }
        return out.join("");
    }
})(atlas || {});
