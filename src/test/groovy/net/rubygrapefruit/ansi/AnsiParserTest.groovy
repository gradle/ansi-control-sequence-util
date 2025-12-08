package net.rubygrapefruit.ansi

import net.rubygrapefruit.ansi.token.*
import spock.lang.Specification
import spock.lang.Unroll

class AnsiParserTest extends Specification {
    def nonAscii = "-√æず∫ʙぴ₦ガき∆ç√∫"
    def parser = new AnsiParser()
    def visitor = new TestVisitor()

    def "parses an empty stream"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes(""))

        then:
        visitor.tokens.empty
    }

    def "parses a single line"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes(nonAscii))

        then:
        visitor.tokens.size() == 1
        visitor.tokens[0] instanceof Text
        visitor.tokens[0].text == nonAscii
    }

    def "parses multiple lines separated by new-line"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes("one\ntwo\n\nthree"))

        then:
        visitor.tokens.size() == 6
        visitor.tokens[0] instanceof Text
        visitor.tokens[0].text == "one"
        visitor.tokens[1] instanceof NewLine
        visitor.tokens[2] instanceof Text
        visitor.tokens[2].text == "two"
        visitor.tokens[3] instanceof NewLine
        visitor.tokens[4] instanceof NewLine
        visitor.tokens[5] instanceof Text
        visitor.tokens[5].text == "three"
    }

    def "parses multiple lines separated by new-line and cr"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes("one\r\ntwo\r\n\r\nthree"))

        then:
        visitor.tokens.size() == 9
        visitor.tokens[0] instanceof Text
        visitor.tokens[0].text == "one"
        visitor.tokens[1] instanceof CarriageReturn
        visitor.tokens[2] instanceof NewLine
        visitor.tokens[3] instanceof Text
        visitor.tokens[3].text == "two"
        visitor.tokens[4] instanceof CarriageReturn
        visitor.tokens[5] instanceof NewLine
        visitor.tokens[6] instanceof CarriageReturn
        visitor.tokens[7] instanceof NewLine
        visitor.tokens[8] instanceof Text
        visitor.tokens[8].text == "three"
    }

    def "parses text with a mix of new-line and cr"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes("\rone\ntwo\r\n\n\r"))

        then:
        visitor.tokens.size() == 8
        visitor.tokens[0] instanceof CarriageReturn
        visitor.tokens[1] instanceof Text
        visitor.tokens[1].text == "one"
        visitor.tokens[2] instanceof NewLine
        visitor.tokens[3] instanceof Text
        visitor.tokens[3].text == "two"
        visitor.tokens[4] instanceof CarriageReturn
        visitor.tokens[5] instanceof NewLine
        visitor.tokens[6] instanceof NewLine
        visitor.tokens[7] instanceof CarriageReturn
    }

    def "parses private control sequences"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[?2026h"))
        output.write(bytes("\u001B[?2026l"))
        output.write(bytes("\u001B[?1004h"))
        output.write(bytes("\u001B[?1004l"))

        then:
        visitor.tokens.size() == 4
        visitor.tokens[0] instanceof BeginSynchronizedUpdate
        visitor.tokens[1] instanceof EndSynchronizedUpdate
        visitor.tokens[2] instanceof UnrecognizedControlSequence
        visitor.tokens[2].sequence == "[?1004h"
        visitor.tokens[3] instanceof UnrecognizedControlSequence
        visitor.tokens[3].sequence == "[?1004l"
    }

    def "parses cursor movement control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[A"))
        output.write(bytes("\u001B[12A"))
        output.write(bytes("\u001B[004A"))
        output.write(bytes("\u001B[B"))
        output.write(bytes("\u001B[12B"))
        output.write(bytes("\u001B[C"))
        output.write(bytes("\u001B[33C"))
        output.write(bytes("\u001B[D"))
        output.write(bytes("\u001B[20D"))
        output.write(bytes("\u001B[G"))
        output.write(bytes("\u001B[1G"))
        output.write(bytes("\u001B[20G"))

        then:
        visitor.tokens.size() == 12
        visitor.tokens[0] instanceof CursorUp
        visitor.tokens[0].count == 1
        visitor.tokens[1] instanceof CursorUp
        visitor.tokens[1].count == 12
        visitor.tokens[2] instanceof CursorUp
        visitor.tokens[2].count == 4
        visitor.tokens[3] instanceof CursorDown
        visitor.tokens[3].count == 1
        visitor.tokens[4] instanceof CursorDown
        visitor.tokens[4].count == 12
        visitor.tokens[5] instanceof CursorForward
        visitor.tokens[5].count == 1
        visitor.tokens[6] instanceof CursorForward
        visitor.tokens[6].count == 33
        visitor.tokens[7] instanceof CursorBackward
        visitor.tokens[7].count == 1
        visitor.tokens[8] instanceof CursorBackward
        visitor.tokens[8].count == 20
        visitor.tokens[9] instanceof CursorToColumn
        visitor.tokens[9].count == 1
        visitor.tokens[10] instanceof CursorToColumn
        visitor.tokens[10].count == 1
        visitor.tokens[11] instanceof CursorToColumn
        visitor.tokens[11].count == 20
    }

    def "parses line clear control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[K"))
        output.write(bytes("\u001B[0K"))
        output.write(bytes("\u001B[1K"))
        output.write(bytes("\u001B[2K"))
        output.write(bytes("\u001B[3K"))
        output.write(bytes("\u001B[12K"))

        then:
        visitor.tokens.size() == 6
        visitor.tokens[0] instanceof EraseToEndOfLine
        visitor.tokens[1] instanceof EraseToEndOfLine
        visitor.tokens[2] instanceof EraseToBeginningOfLine
        visitor.tokens[3] instanceof EraseInLine
        visitor.tokens[4] instanceof UnrecognizedControlSequence
        visitor.tokens[4].sequence == "[3K"
        visitor.tokens[4] instanceof UnrecognizedControlSequence
        visitor.tokens[5].sequence == "[12K"
    }

    def "parses text attribute control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[m"))
        output.write(bytes("\u001B[0m"))
        output.write(bytes("\u001B[2m"))
        output.write(bytes("\u001B[99m"))
        output.write(bytes("\u001B[0;2m"))
        output.write(bytes("\u001B[99;0m"))
        output.write(bytes("\u001B[;0m"))
        output.write(bytes("\u001B[0;m"))
        output.write(bytes("\u001B[;m"))
        output.write(bytes("\u001B[;;;;m"))
        output.write(bytes("\u001B[1;1;;22m"))

        then:
        visitor.tokens.size() == 15
        visitor.tokens[0] instanceof ForegroundColor
        visitor.tokens[0].color == TextColor.DEFAULT
        visitor.tokens[1] instanceof BackgroundColor
        visitor.tokens[1].color == TextColor.DEFAULT
        visitor.tokens[2] instanceof BoldOff
        visitor.tokens[3] instanceof ForegroundColor
        visitor.tokens[3].color == TextColor.DEFAULT
        visitor.tokens[4] instanceof BackgroundColor
        visitor.tokens[4].color == TextColor.DEFAULT
        visitor.tokens[5] instanceof BoldOff
        visitor.tokens[6] instanceof UnrecognizedControlSequence
        visitor.tokens[6].sequence == "[2m"
        visitor.tokens[7] instanceof UnrecognizedControlSequence
        visitor.tokens[7].sequence == "[99m"
        visitor.tokens[8] instanceof UnrecognizedControlSequence
        visitor.tokens[8].sequence == "[0;2m"
        visitor.tokens[9] instanceof UnrecognizedControlSequence
        visitor.tokens[9].sequence == "[99;0m"
        visitor.tokens[10] instanceof UnrecognizedControlSequence
        visitor.tokens[10].sequence == "[;0m"
        visitor.tokens[11] instanceof UnrecognizedControlSequence
        visitor.tokens[11].sequence == "[0;m"
        visitor.tokens[12] instanceof UnrecognizedControlSequence
        visitor.tokens[12].sequence == "[;m"
        visitor.tokens[13] instanceof UnrecognizedControlSequence
        visitor.tokens[13].sequence == "[;;;;m"
        visitor.tokens[14] instanceof UnrecognizedControlSequence
        visitor.tokens[14].sequence == "[1;1;;22m"
    }

    def "parses foreground color control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes(sequence))

        then:
        visitor.tokens.size() == 1
        visitor.tokens[0] instanceof ForegroundColor
        visitor.tokens[0].color.name == colour
        !visitor.tokens[0].color.bright

        where:
        sequence     | colour
        "\u001B[30m" | "black"
        "\u001B[31m" | "red"
        "\u001B[32m" | "green"
        "\u001B[33m" | "yellow"
        "\u001B[34m" | "blue"
        "\u001B[35m" | "magenta"
        "\u001B[36m" | "cyan"
        "\u001B[37m" | "white"
        "\u001B[39m" | null
    }

    def "parses high intensity foreground color control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes(sequence))

        then:
        visitor.tokens.size() == 1
        visitor.tokens[0] instanceof ForegroundColor
        visitor.tokens[0].color.name == colour
        visitor.tokens[0].color.bright

        where:
        sequence     | colour
        "\u001B[90m" | "black"
        "\u001B[91m" | "red"
        "\u001B[92m" | "green"
        "\u001B[93m" | "yellow"
        "\u001B[94m" | "blue"
        "\u001B[95m" | "magenta"
        "\u001B[96m" | "cyan"
        "\u001B[97m" | "white"
    }

    def "parses background color control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes(sequence))

        then:
        visitor.tokens.size() == 1
        visitor.tokens[0] instanceof BackgroundColor
        visitor.tokens[0].color.name == colour
        !visitor.tokens[0].color.bright

        where:
        sequence     | colour
        "\u001B[40m" | "black"
        "\u001B[41m" | "red"
        "\u001B[42m" | "green"
        "\u001B[43m" | "yellow"
        "\u001B[44m" | "blue"
        "\u001B[45m" | "magenta"
        "\u001B[46m" | "cyan"
        "\u001B[47m" | "white"
        "\u001B[49m" | null
    }

    def "parses high intensity background color control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes(sequence))

        then:
        visitor.tokens.size() == 1
        visitor.tokens[0] instanceof BackgroundColor
        visitor.tokens[0].color.name == colour
        visitor.tokens[0].color.bright

        where:
        sequence      | colour
        "\u001B[100m" | "black"
        "\u001B[101m" | "red"
        "\u001B[102m" | "green"
        "\u001B[103m" | "yellow"
        "\u001B[104m" | "blue"
        "\u001B[105m" | "magenta"
        "\u001B[106m" | "cyan"
        "\u001B[107m" | "white"
    }

    def "parses bold text attribute control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[1m"))
        output.write(bytes("\u001B[22m"))
        output.write(bytes("\u001B[01m"))

        then:
        visitor.tokens.size() == 3
        visitor.tokens[0] instanceof BoldOn
        visitor.tokens[1] instanceof BoldOff
        visitor.tokens[2] instanceof BoldOn
    }

    def "parses composite text attribute control sequence"() {
        when:
        def output = parser.newParser("utf-8", visitor)
        output.write(bytes("\u001B[0;31m"))
        output.write(bytes("\u001B[32;1m"))
        output.write(bytes("\u001B[39;22m"))
        output.write(bytes("\u001B[22;39;32;1m"))
        output.write(bytes("\u001B[0;01m"))

        then:
        visitor.tokens.size() == 16
        visitor.tokens[0] instanceof ForegroundColor
        visitor.tokens[0].color == TextColor.DEFAULT
        visitor.tokens[1] instanceof BackgroundColor
        visitor.tokens[1].color == TextColor.DEFAULT
        visitor.tokens[2] instanceof BoldOff
        visitor.tokens[3] instanceof ForegroundColor
        visitor.tokens[3].color == TextColor.RED
        visitor.tokens[4] instanceof ForegroundColor
        visitor.tokens[4].color == TextColor.GREEN
        visitor.tokens[5] instanceof BoldOn
        visitor.tokens[6] instanceof ForegroundColor
        visitor.tokens[6].color == TextColor.DEFAULT
        visitor.tokens[7] instanceof BoldOff
        visitor.tokens[8] instanceof BoldOff
        visitor.tokens[9] instanceof ForegroundColor
        visitor.tokens[9].color == TextColor.DEFAULT
        visitor.tokens[10] instanceof ForegroundColor
        visitor.tokens[10].color == TextColor.GREEN
        visitor.tokens[11] instanceof BoldOn
        visitor.tokens[12] instanceof ForegroundColor
        visitor.tokens[12].color == TextColor.DEFAULT
        visitor.tokens[13] instanceof BackgroundColor
        visitor.tokens[13].color == TextColor.DEFAULT
        visitor.tokens[14] instanceof BoldOff
        visitor.tokens[15] instanceof BoldOn
    }

    @Unroll
    def "parses control sequence - #sequence"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes("\u001B" + sequence + "abc\u001B" + sequence + "\n"))

        then:
        visitor.tokens.size() == 4
        visitor.tokens[0] instanceof UnrecognizedControlSequence
        visitor.tokens[0].sequence == sequence
        visitor.tokens[1] instanceof Text
        visitor.tokens[1].text == "abc"
        visitor.tokens[2] instanceof UnrecognizedControlSequence
        visitor.tokens[2].sequence == sequence
        visitor.tokens[3] instanceof NewLine

        where:
        sequence    | _
        '[q'        | _
        '[1Q'       | _
        '[01234q'   | _
        '[12Q'      | _
        '[1;2m'     | _
        '[123;245m' | _
        '[;2m'      | _
        '[;234m'    | _
        '[2;m'      | _
        '[234;m'    | _
        '[;m'       | _
    }

    @Unroll
    def "parses partial sequence - #sequence"() {
        when:
        parser.newParser("utf-8", visitor).write(bytes("\u001B" + sequence))

        then:
        visitor.tokens.size() == 2
        visitor.tokens[0] instanceof UnrecognizedControlSequence
        visitor.tokens[0].sequence == expected
        visitor.tokens[1] instanceof Text
        visitor.tokens[1].text == delim

        where:
        sequence | expected | delim
        'a'      | ''       | 'a'
        '1;2abc' | ''       | '1;2abc'
        '[ '     | '['      | ' '
        '[_'     | '['      | '_'
        '[['     | '['      | '['
        '[a1'    | '[a'     | '1'
        '[a;'    | '[a'     | ';'
        '[1;m2'  | '[1;m'   | '2'
    }

    byte[] bytes(String str) {
        return str.getBytes("utf-8")
    }

    static class TestVisitor implements Visitor {
        def tokens = []

        @Override
        void visit(Token token) {
            tokens << token
        }
    }
}
