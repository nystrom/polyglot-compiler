package polyglot.ext.pao.runtime;

public class Integer extends Long
{
  public Integer( int value)
  {
    super(value);
  }

  public int intValue()
  {
    return (int) value;
  } 
}
