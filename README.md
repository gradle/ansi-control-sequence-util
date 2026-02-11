A Java library of utilities for parsing and interpreting text that contains ANSI control sequences.

This can be used to implement a terminal emulator or for testing or documenting command-line applications.

## Usage

```
repositories { 
    mavenCentral()
}

dependencies {
    implementation 'com.gradle:ansi-control-sequence-util:0.5'
}
```

## TODO

### Missing emulation

Not all control sequences are supported yet: 

- Unsupported cursor movement control sequences:
    - Cursor next line
    - Cursor previous line
    - Cursor position
    - Scroll up
    - Scroll down
    - Save cursor position
    - Restore cursor position
- Unsupported erase control sequences:
    - Erase part of screen
- Unsupported text attribute control sequences:
    - Background color is not fully supported
    - Handle 256 color
    - Handle 24-bit colors
    - Handle underline

### Improvements

- Render unrecognized control sequences in `AnsiConsole` and `HtmlFormatter` in some highly visible way.
- `HtmlFormatter` improvements:
    - Background color should be used for remainder of row when new-line is emitted?
    - Should stream to an `OutputStream`.
    - Escape text content.
- `AnsiConsole` improvements:
    - Erase to end of line should erase character under cursor
    - Support background color: erase should fill with background color
    - When replacing tail of span with another span, check whether next span has target attributes already
    - When overwriting span contents at offset 0 of span, maybe merge with previous
    - When erasing bold span that is adjacent to non-bold span
- `DiagnosticConsole` improvements:
    - `contents()` should split lines.
- Replace usages of private `ForegroundColor` constructor from tests.
- Collect text attribute and color diagnostics into their respective classes.
- Add a strongly typed visitor that accepts only text tokens
- Add a strongly typed visitor that accepts only text and text attribute tokens
