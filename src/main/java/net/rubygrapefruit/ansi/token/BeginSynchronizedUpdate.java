package net.rubygrapefruit.ansi.token;

public class BeginSynchronizedUpdate extends ControlSequence {
    public static final BeginSynchronizedUpdate INSTANCE = new BeginSynchronizedUpdate();

    private BeginSynchronizedUpdate() {
    }

    @Override
    public void appendDiagnostic(StringBuilder builder) {
        builder.append("{begin-synchronized-update}");
    }
}
