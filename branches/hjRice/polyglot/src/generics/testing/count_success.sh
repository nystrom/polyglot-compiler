NBTEST=`ls *.jl5 | wc -l | sed s/' '//g`
TC=`grep -c "TYPECHECKING COMPARED AS EXPECTED!" $1`
CG=`grep -c "CODEGEN COMPARED AS EXPECTED!" $1`
echo "Type check: $TC / $NBTEST" 
echo "Code gen  : $CG / $NBTEST" 


