package net.rubygrapefruit.ansi.token;

public class EndSynchronizedUpdate extends ControlSequence {
    public static final EndSynchronizedUpdate INSTANCE = new EndSynchronizedUpdate();

    private EndSynchronizedUpdate() {
    }

    @Override
    public void appendDiagnostic(StringBuilder builder) {
        builder.append("{end-synchronized-update}");
    }
}
