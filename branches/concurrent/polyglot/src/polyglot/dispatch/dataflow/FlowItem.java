package polyglot.dispatch.dataflow;

public interface FlowItem<I extends FlowItem<I>> {
    I mergeWith(I that);
}
