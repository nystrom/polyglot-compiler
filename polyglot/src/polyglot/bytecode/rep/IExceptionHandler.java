package polyglot.bytecode.rep;

import polyglot.bytecode.types.Type;

public interface IExceptionHandler {

    Type getCatchType();

    ILabel getEndPC();

    ILabel getHandlerPC();

    ILabel getStartPC();

    void setCatchType(Type catchType);

    void setEndPC(ILabel endPC);

    void setHandlerPC(ILabel handlerPC);

    void setStartPC(ILabel startPC);

}