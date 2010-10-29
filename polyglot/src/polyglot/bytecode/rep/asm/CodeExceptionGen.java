/**
 * 
 */
package polyglot.bytecode.rep.asm;

import polyglot.bytecode.rep.IExceptionHandler;
import polyglot.bytecode.rep.ILabel;
import polyglot.bytecode.types.Type;

public class CodeExceptionGen implements IExceptionHandler {

    public ILabel startPC;
    public ILabel endPC;
    public ILabel handlerPC;
    public Type catchType;

    public CodeExceptionGen(ILabel startPc, ILabel endPc, ILabel handlerPc, Type catchType) {
        this.startPC = startPc;
        this.endPC = endPc;
        this.handlerPC = handlerPc;
        this.catchType = catchType;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#getCatchType()
     */
    public Type getCatchType() {
        return catchType;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#getEndPC()
     */
    public ILabel getEndPC() {
        return endPC;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#getHandlerPC()
     */
    public ILabel getHandlerPC() {
        return handlerPC;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#getStartPC()
     */
    public ILabel getStartPC() {
        return startPC;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#setCatchType(polyglot.bytecode.types.Type)
     */
    public void setCatchType(Type catchType) {
        this.catchType = catchType;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#setEndPC(polyglot.bytecode.rep.ILabel)
     */
    public void setEndPC(ILabel endPC) {
        this.endPC = endPC;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#setHandlerPC(polyglot.bytecode.rep.ILabel)
     */
    public void setHandlerPC(ILabel handlerPC) {
        this.handlerPC = handlerPC;
    }

    /* (non-Javadoc)
     * @see polyglot.bytecode.rep.IExceptionHandler#setStartPC(polyglot.bytecode.rep.ILabel)
     */
    public void setStartPC(ILabel startPC) {
        this.startPC = startPC;
    }
}