/* The following code was generated by JFlex 1.4.3 on 14.10.11 19:37 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package net.dfranek.typoscript.lexer;

import java.util.regex.Pattern;
import org.netbeans.spi.lexer.LexerInput;
import org.netbeans.spi.lexer.LexerRestartInfo;

/**
 * 
 */
public class TSScanner {

    /* user code: */
    private LexerInput input;
    private boolean inValue = false;
    private boolean inComment = false;
    private boolean regexp = false;

    public TSScanner(LexerRestartInfo info) {
        this.input = info.input();
    }

    /**
     * Resumes scanning until the next regular expression is matched,
     * the end of input is encountered or an I/O-Error occurs.
     *
     * @return      the next token
     * @exception   java.io.IOException  if any I/O-Error occurs
     */
    public TSTokenId nextToken() throws java.io.IOException {
        TSTokenId token = null;
        char ch = (char) input.read();

        if (ch == '\n') {
            token = TSTokenId.TS_NL;
            inValue = false;
        } else if (!this.inValue && this.inComment) {
            token = readMultilineComment(ch);
        } else if (isWhiteSpace(ch)) {
            nextWhileWhiteSpace();
            return TSTokenId.WHITESPACE;
        } else if (!this.inValue && (ch == '"' || ch == '\'')) {
            nextUntilUnescaped(ch);
            token = TSTokenId.TS_STRING;
        } else if ((ch == '<'
                || ch == '>'
                || (ch == '='
                && (char) input.read() != '<'))
                && (char) input.read() != '\n') { // there must be some value behind the operator!
            this.inValue = true;
            token = TSTokenId.TS_OPERATOR;
        } else if (!this.inValue && ch == '[') {
            nextUntilUnescaped(']');
            token = TSTokenId.TS_CONDITION;
            // with punctuation, the type of the token is the symbol itself
        } else if (!this.inValue && Pattern.matches("[\\[\\]\\(\\),;\\:\\.\\<\\>\\=]", new Character(ch).toString())) {
            token = TSTokenId.TS_OPERATOR;
        } else if (!this.inValue && (ch == '{' || ch == '}')) {
            token = TSTokenId.TS_CURLY;
        } else if (!this.inValue && ch == '0' && (input.read() == 'x' || input.read() == 'X')) {
            token = readHexNumber();
        } else if (!this.inValue && isDigit(new Character(ch).toString())) {
            token = readNumber();
        } else if (!this.inValue && ch == '/') {
            char next = (char) input.read();

            if (next == '*') {
                token = readMultilineComment(ch);

            } else if (next == '/') {
                nextUntilUnescaped('\n');
                token = TSTokenId.TS_COMMENT;

            } else if (this.regexp) {
                token = readRegexp();

            } else {
                nextWhileOperatorChar();
                token = TSTokenId.TS_OPERATOR;
            }

        } else if (!this.inValue && ch == '#') {
            nextUntilUnescaped('\n');
            token = TSTokenId.TS_COMMENT;
        } else if (!this.inValue && isOperatorChar(new Character(ch).toString())) {
            nextWhileOperatorChar();
            token = TSTokenId.TS_OPERATOR;
        } else {
            nextWhileWordChar();
            token = TSTokenId.TS_VALUE;
        }
        return token;
    }

    protected TSTokenId readRegexp() {
        nextUntilUnescaped('/');
        nextWhileMatchesRegExp("[gi]");
        return TSTokenId.TS_REGEXP;
    }

    protected void nextWhileWordChar(){
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && isWordChar(new Character(next).toString())) {
        }
    }
    
    protected void nextWhileMatchesRegExp(String pattern) {
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && Pattern.matches(pattern, new Character(next).toString())) {
        }
    }

    protected TSTokenId readNumber() {
        nextWhileDigit();
        return TSTokenId.TS_NUMBER;
    }

    protected TSTokenId readHexNumber() {
        input.read();
        // skip the 'x'
        nextWhileHexDigit();

        return TSTokenId.TS_NUMBER;
    }

    private void nextWhileOperatorChar() {
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && isOperatorChar(new Character(next).toString())) {
        }
    }

    private void nextWhileDigit() {
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && isDigit(new Character(next).toString())) {
        }
    }

    protected void nextWhileHexDigit() {
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && isHexDigit(new Character(next).toString())) {
        }
    }

    protected void nextWhileWhiteSpace() {
        char next;
        while (((next = (char) input.read()) != LexerInput.EOF) && isWhiteSpace(next)) {
        }
    }

    protected void nextUntilUnescaped(char end) {
        boolean escaped = false;
        char next = (char) input.read();
        while (((next = (char) input.read()) != LexerInput.EOF) && next != '\n') {
            input.read();
            if (next == end && !escaped) {
                break;
            }
            escaped = next == '\\';
        }
    }

    protected TSTokenId readMultilineComment(char start) {
        this.inComment = true;
        boolean maybeEnd = (start == '*');
        while (true) {
            char next = (char) input.read();
            if (next == '\n') {
                break;
            }

            if (next == '/' && maybeEnd) {
                this.inComment = false;
                break;
            }
            maybeEnd = (next == '*');
        }

        return TSTokenId.TS_COMMENT;
    }

    protected boolean isOperatorChar(String input) {
        return Pattern.matches("[\\+\\-\\*\\&\\%\\/=<>!\\?]", input);
    }

    protected boolean isDigit(String input) {
        return Pattern.matches("[0-9]", input);
    }

    protected boolean isHexDigit(String input) {
        return Pattern.matches("[0-9A-Fa-f]", input);
    }

    protected boolean isWordChar(String input) {
        return Pattern.matches("[\\w\\$_]", input);
    }

    protected boolean isWhiteSpace(char ch) {
        return ch != '\n' && (ch == ' ' || Pattern.matches("\\s", new Character(ch).toString()));
    }
}
