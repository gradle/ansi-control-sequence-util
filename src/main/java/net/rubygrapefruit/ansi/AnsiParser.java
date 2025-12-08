package net.rubygrapefruit.ansi;

import net.rubygrapefruit.ansi.token.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a stream of bytes into a stream of {@link Token} instances.
 */
public class AnsiParser {
    /**
     * Creates an {@link OutputStream} that parses the bytes written to it into a stream of {@link Token} instances.
     *
     * <p>The {@link OutputStream} is not thread-safe.</p>
     *
     * @param charset The charset that the text has been encoded with.
     * @param visitor The visitor to receive the tokens
     */
    public OutputStream newParser(String charset, Visitor visitor) {
        try {
            Charset encoding = Charset.forName(charset);
            return new ParsingStream(new AnsiByteConsumer(encoding, visitor));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse input", e);
        }
    }

    private static void split(String string, Visitor visitor) {
        int pos = 0;
        while (pos < string.length()) {
            if (string.charAt(pos) == '\n') {
                visitor.visit(NewLine.INSTANCE);
                pos++;
            } else if (string.charAt(pos) == '\r') {
                visitor.visit(CarriageReturn.INSTANCE);
                pos++;
            } else {
                int endToken = pos;
                while (endToken < string.length() && string.charAt(endToken) != '\n' && string.charAt(endToken) != '\r') {
                    endToken++;
                }
                visitor.visit(new Text(string.substring(pos, endToken)));
                pos = endToken;
            }
        }
    }

    private static boolean parse(String params, char code, Visitor visitor) {
        if (code == 'A') {
            if (params.isEmpty()) {
                visitor.visit(new CursorUp(1));
            } else {
                visitor.visit(new CursorUp(Integer.parseInt(params)));
            }
            return true;
        }
        if (code == 'B') {
            if (params.isEmpty()) {
                visitor.visit(new CursorDown(1));
            } else {
                visitor.visit(new CursorDown(Integer.parseInt(params)));
            }
            return true;
        }
        if (code == 'C') {
            if (params.isEmpty()) {
                visitor.visit(new CursorForward(1));
            } else {
                visitor.visit(new CursorForward(Integer.parseInt(params)));
            }
            return true;
        }
        if (code == 'D') {
            if (params.isEmpty()) {
                visitor.visit(new CursorBackward(1));
            } else {
                visitor.visit(new CursorBackward(Integer.parseInt(params)));
            }
            return true;
        }
        if (code == 'K') {
            if (params.isEmpty() || params.equals("0")) {
                visitor.visit(EraseToEndOfLine.INSTANCE);
                return true;
            }
            if (params.equals("1")) {
                visitor.visit(EraseToBeginningOfLine.INSTANCE);
                return true;
            }
            if (params.equals("2")) {
                visitor.visit(EraseInLine.INSTANCE);
                return true;
            }
        }
        if (code == 'G') {
            if (params.isEmpty() || params.equals("1")) {
                visitor.visit(new CursorToColumn(1));
            } else {
                visitor.visit(new CursorToColumn(Integer.parseInt(params)));
            }
            return true;
        }
        if (code == 'm') {
            if (params.isEmpty()) {
                visitor.visit(ForegroundColor.DEFAULT);
                visitor.visit(BackgroundColor.DEFAULT);
                visitor.visit(BoldOff.INSTANCE);
                return true;
            }
            if (params.contains(";")) {
                int pos = 0;
                CollectingVisitor collectingVisitor = new CollectingVisitor();
                while (pos < params.length()) {
                    int end = params.indexOf(';', pos);
                    if (end < 0) {
                        end = params.length();
                    } else if (end == pos) {
                        return false;
                    }
                    String attr = params.substring(pos, end);
                    if (!parseTextAttribute(attr, collectingVisitor)) {
                        return false;
                    }
                    if (end == params.length() - 1) {
                        return false;
                    }
                    pos = end + 1;
                }
                for (Token token : collectingVisitor.tokens) {
                    visitor.visit(token);
                }
                return true;
            }
            if (parseTextAttribute(params, visitor)) {
                return true;
            }
        }
        if (code == 'h') {
            if ("?2026".equals(params)) {
                visitor.visit(BeginSynchronizedUpdate.INSTANCE);
                return true;
            }
        }
        if (code == 'l') {
            if ("?2026".equals(params)) {
                visitor.visit(EndSynchronizedUpdate.INSTANCE);
                return true;
            }
        }
        return false;
    }

    private static boolean parseTextAttribute(String params, Visitor visitor) {
        if (params.equals("0")) {
            visitor.visit(ForegroundColor.DEFAULT);
            visitor.visit(BackgroundColor.DEFAULT);
            visitor.visit(BoldOff.INSTANCE);
            return true;
        }
        if (params.equals("01") || params.equals("1")) {
            visitor.visit(BoldOn.INSTANCE);
            return true;
        }
        if (params.equals("22")) {
            visitor.visit(BoldOff.INSTANCE);
            return true;
        }
        if (params.equals("39")) {
            visitor.visit(ForegroundColor.DEFAULT);
            return true;
        }
        if (params.equals("30")) {
            visitor.visit(ForegroundColor.of(TextColor.BLACK));
            return true;
        }
        if (params.equals("31")) {
            visitor.visit(ForegroundColor.of(TextColor.RED));
            return true;
        }
        if (params.equals("32")) {
            visitor.visit(ForegroundColor.of(TextColor.GREEN));
            return true;
        }
        if (params.equals("33")) {
            visitor.visit(ForegroundColor.of(TextColor.YELLOW));
            return true;
        }
        if (params.equals("34")) {
            visitor.visit(ForegroundColor.of(TextColor.BLUE));
            return true;
        }
        if (params.equals("35")) {
            visitor.visit(ForegroundColor.of(TextColor.MAGENTA));
            return true;
        }
        if (params.equals("36")) {
            visitor.visit(ForegroundColor.of(TextColor.CYAN));
            return true;
        }
        if (params.equals("37")) {
            visitor.visit(ForegroundColor.of(TextColor.WHITE));
            return true;
        }
        if (params.equals("90")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_BLACK));
            return true;
        }
        if (params.equals("91")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_RED));
            return true;
        }
        if (params.equals("92")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_GREEN));
            return true;
        }
        if (params.equals("93")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_YELLOW));
            return true;
        }
        if (params.equals("94")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_BLUE));
            return true;
        }
        if (params.equals("95")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_MAGENTA));
            return true;
        }
        if (params.equals("96")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_CYAN));
            return true;
        }
        if (params.equals("97")) {
            visitor.visit(ForegroundColor.of(TextColor.BRIGHT_WHITE));
            return true;
        }
        if (params.equals("40")) {
            visitor.visit(BackgroundColor.of(TextColor.BLACK));
            return true;
        }
        if (params.equals("41")) {
            visitor.visit(BackgroundColor.of(TextColor.RED));
            return true;
        }
        if (params.equals("42")) {
            visitor.visit(BackgroundColor.of(TextColor.GREEN));
            return true;
        }
        if (params.equals("43")) {
            visitor.visit(BackgroundColor.of(TextColor.YELLOW));
            return true;
        }
        if (params.equals("44")) {
            visitor.visit(BackgroundColor.of(TextColor.BLUE));
            return true;
        }
        if (params.equals("45")) {
            visitor.visit(BackgroundColor.of(TextColor.MAGENTA));
            return true;
        }
        if (params.equals("46")) {
            visitor.visit(BackgroundColor.of(TextColor.CYAN));
            return true;
        }
        if (params.equals("47")) {
            visitor.visit(BackgroundColor.of(TextColor.WHITE));
            return true;
        }
        if (params.equals("49")) {
            visitor.visit(BackgroundColor.DEFAULT);
            return true;
        }
        if (params.equals("100")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_BLACK));
            return true;
        }
        if (params.equals("101")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_RED));
            return true;
        }
        if (params.equals("102")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_GREEN));
            return true;
        }
        if (params.equals("103")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_YELLOW));
            return true;
        }
        if (params.equals("104")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_BLUE));
            return true;
        }
        if (params.equals("105")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_MAGENTA));
            return true;
        }
        if (params.equals("106")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_CYAN));
            return true;
        }
        if (params.equals("107")) {
            visitor.visit(BackgroundColor.of(TextColor.BRIGHT_WHITE));
            return true;
        }

        return false;
    }

    private static class ParsingStream extends OutputStream {
        private final AnsiByteConsumer sink;

        ParsingStream(AnsiByteConsumer sink) {
            this.sink = sink;
        }

        @Override
        public void write(int i) throws IOException {
            byte[] buffer = new byte[1];
            buffer[0] = (byte) i;
            write(buffer, 0, 1);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            write(bytes, 0, bytes.length);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            synchronized (sink) {
                sink.consume(new Buffer(bytes, offset, length));
            }
        }

        @Override
        public void flush() throws IOException {
        }

        @Override
        public void close() throws IOException {
        }
    }

    enum State {
        Normal, LeftParen, Param, Code
    }

    private static class AnsiByteConsumer {
        private final StringBuilder currentSequence = new StringBuilder();
        private final Charset charset;
        private final Visitor visitor;
        private State state = State.Normal;

        AnsiByteConsumer(Charset charset, Visitor visitor) {
            this.charset = charset;
            this.visitor = visitor;
        }

        void consume(Buffer buffer) {
            while (buffer.hasMore()) {
                switch (state) {
                    case LeftParen:
                        if (buffer.peek() != '[') {
                            visitor.visit(new UnrecognizedControlSequence(""));
                            state = State.Normal;
                        } else {
                            buffer.consume();
                            state = State.Param;
                        }
                        break;
                    case Param:
                        byte nextDigit = buffer.peek();
                        if ((nextDigit < '0' || nextDigit > '9') && nextDigit != ';' && nextDigit != '?') {
                            state = State.Code;
                        } else {
                            currentSequence.append((char) nextDigit);
                            buffer.consume();
                        }
                        break;
                    case Code:
                        char next = (char) buffer.peek();
                        if (next >= 'a' && next <= 'z' || next >= 'A' && next <= 'Z') {
                            buffer.consume();
                            if (!parse(currentSequence.toString(), next, visitor)) {
                                visitor.visit(new UnrecognizedControlSequence("[" + currentSequence.toString() + next));
                            }
                        } else {
                            visitor.visit(new UnrecognizedControlSequence("[" + currentSequence.toString()));
                        }
                        currentSequence.setLength(0);
                        state = State.Normal;
                        break;
                    case Normal:
                        Buffer prefix = buffer.consumeToNext((byte) 27);
                        if (prefix == null) {
                            split(buffer.consumeString(charset), visitor);
                            return;
                        }
                        split(prefix.consumeString(charset), visitor);
                        state = State.LeftParen;
                        buffer.consume();
                        currentSequence.setLength(0);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }
    }

    private static class Buffer {
        private final byte[] buffer;
        private int offset;
        private int length;

        Buffer(byte[] buffer, int offset, int length) {
            this.buffer = buffer;
            this.offset = offset;
            this.length = length;
        }

        String consumeString(Charset charset) {
            return new String(buffer, offset, length, charset);
        }

        byte peek() {
            return buffer[offset];
        }

        void consume() {
            offset++;
            length--;
        }

        boolean hasMore() {
            return length > 0;
        }

        Buffer consumeToNext(byte value) {
            int maxOffset = offset + length;
            for (int nextValue = offset; nextValue < maxOffset; nextValue++) {
                if (buffer[nextValue] == value) {
                    int count = nextValue - offset;
                    Buffer result = new Buffer(buffer, offset, count);
                    offset = nextValue;
                    length -= count;
                    return result;
                }
            }
            return null;
        }
    }

    private static class CollectingVisitor implements Visitor {
        final List<Token> tokens = new ArrayList<>();

        @Override
        public void visit(Token token) {
            tokens.add(token);
        }
    }
}
