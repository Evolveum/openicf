/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
using System;
using System.Text;
using System.Collections.Generic;
using Org.IdentityConnectors.Common;
using Org.IdentityConnectors.Framework.Common.Objects;
namespace Org.IdentityConnectors.Framework.Common.Objects.Filters
{
    #region AbstractFilterTranslator
    /**
     * Base class to make it easier to implement Search. 
     * A search filter may contain operators (such as 'contains' or 'in')
     * or may contain logical operators (such as 'AND', 'OR' or 'NOT')
     * that a connector cannot implement using the native API
     * of the target system or application. 
     * A connector developer should subclass <code>AbstractFilterTranslator</code>
     * in order to declare which filter operations the connector does support.
     * This allows the <code>FilterTranslator</code> instance to analyze
     * a specified search filter and reduce the filter to its most efficient form.
     * The default (and worst-case) behavior is to return a null expression, 
     * which means that the connector should return "everything" 
     * (that is, should return all values for every requested attribute)
     * and rely on the common code in the framework to perform filtering.
     * This "fallback" behavior is good (in that it ensures consistency
     * of search behavior across connector implementations) but it is 
     * obviously better for performance and scalability if each connector
     * performs as much filtering as the native API of the target can support.
     * <p> 
     * A subclass should override each of the following methods where possible:
     * <ol>
     *    <li>{@link #createAndExpression}</li>
     *    <li>{@link #createOrExpression}</li>
     *    <li>{@link #createContainsExpression(ContainsFilter, boolean)}</li>
     *    <li>{@link #createEndsWithExpression(EndsWithFilter, boolean)}</li>
     *    <li>{@link #createEqualsExpression(EqualsFilter, boolean)}</li>
     *    <li>{@link #createGreaterThanExpression(GreaterThanFilter, boolean)}</li>
     *    <li>{@link #createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter, boolean)}</li>
     *    <li>{@link #createStartsWithExpression(StartsWithFilter, boolean)}</li>
     * </ol>
     * <p>
     * Translation can then be performed using {@link #translate(Filter)}.
     * <p>
     * @param <T> The result type of the translator. Commonly this will
     * be a string, but there are cases where you might need to return
     * a more complex data structure. For example if you are building a SQL
     * query, you will need not *just* the base WHERE clause but a list
     * of tables that need to be joined together.
     */
    abstract public class AbstractFilterTranslator<T> : FilterTranslator<T>
        where T : class
    { 
        
        /**
         * Main method to be called to translate a filter
         * @param filter The filter to translate.
         * @return The list of queries to be performed. The list
         * <code>size()</code> may be one of the following:
         * <ol>
         *    <li>0 - This
         *    signifies <b>fetch everything</b>. This may occur if your filter 
         *    was null or one of your <code>create*</code> methods returned null.</li>
         *    <li>1 - List contains a single query that will return the results from the filter.
         *    Note that the results may be a <b>superset</b> of those specified by
         *    the filter in the case that one of your <code>create*</code> methods returned null.
         *    That is OK from a behavior standpoint since <code>ConnectorFacade</code> performs
         *    a second level of filtering. However it is undesirable from a performance standpoint. </li>
         *    <li>>1 - List contains multiple queries that must be performed in order to
         *    meet the filter that was passed in. Note that this only occurs if your
         *    {@link #createOrExpression} method can return null. If this happens, it
         *    is the responsibility of the connector implementor to perform each query
         *    and combine the results. In order to eliminate duplicates, the connector
         *    implementation must keep an in-memory <code>HashSet</code> of those UID
         *    that have been visited thus far. This will not scale well if your
         *    result sets are large. Therefore it is <b>recommended</b> that if
         *    at all possible you implement {@link #createOrExpression}</li>
         * </ol>
         */
        public IList<T> Translate(Filter filter) {
            if ( filter == null ) {
                return new List<T>();
            }
            //this must come first
            filter = NormalizeNot(filter);
            filter = SimplifyAndDistribute(filter);
            //might have simplified it to the everything filter
            if ( filter == null ) {
                return new List<T>();
            }
            IList<T> result = TranslateInternal(filter);
            //now "optimize" - we can eliminate exact matches at least
            HashSet<T> set = new HashSet<T>();
            IList<T> optimized = new List<T>(result.Count);
            foreach (T obj in result) {
                if ( set.Add(obj) ) {
                    optimized.Add(obj);
                }
            }
            return optimized;
        }
        
        /**
         * Pushes Not's so that they are just before the leaves of the tree
         */
        private Filter NormalizeNot(Filter filter) {
            if ( filter is AndFilter ) {
                AndFilter af = (AndFilter)filter;
                return new AndFilter(NormalizeNot(af.Left),
                        NormalizeNot(af.Right));
            }
            else if ( filter is OrFilter ) {
                OrFilter of = (OrFilter)filter;
                return new OrFilter(NormalizeNot(of.Left),
                        NormalizeNot(of.Right));
            }
            else if ( filter is NotFilter ) {
                NotFilter nf = (NotFilter)filter;
                return Negate(NormalizeNot(nf.Filter));
            }
            else {
                return filter;
            }        
        }
        
        /**
         * Given a filter, create a filter representing its negative.
         * This is used by normalizeNot.
         */
        private Filter Negate(Filter filter)
        {
            if ( filter is AndFilter ) {
                AndFilter af = (AndFilter)filter;
                return new OrFilter(Negate(af.Left),
                                    Negate(af.Right));
            }
            else if ( filter is OrFilter ) {
                OrFilter of = (OrFilter)filter;
                return new AndFilter(Negate(of.Left),
                        Negate(of.Right));
            }
            else if ( filter is NotFilter ) {
                NotFilter nf = (NotFilter)filter;
                return nf.Filter;
            }
            else {
                return new NotFilter(filter);
            }
        }
        
        /**
         * Simultaneously prunes those portions of the
         * filter than cannot be implemented and distributes
         * Ands over Ors where needed if the resource does not
         * implement Or.
         * 
         * @param filter Nots must already be normalized
         * @return a simplified filter or null to represent the
         * "everything" filter.
         */
        private Filter SimplifyAndDistribute(Filter filter) {
            if ( filter is AndFilter ) {
                AndFilter af = (AndFilter)filter;
                Filter simplifiedLeft =
                    SimplifyAndDistribute(af.Left);
                Filter simplifiedRight =
                    SimplifyAndDistribute(af.Right);
                if ( simplifiedLeft == null ) {
                    //left is "everything" - just return the right
                    return simplifiedRight;
                }
                else if ( simplifiedRight == null ) {
                    //right is "everything" - just return the left
                    return simplifiedLeft;
                }
                else {
                    //simulate translation of the left and right
                    //to see where we end up
                    IList<T> leftExprs =
                        TranslateInternal(simplifiedLeft);
                    IList<T> rightExprs =
                        TranslateInternal(simplifiedRight);
                    if (leftExprs.Count == 0) {
                        //This can happen only when one of the create* methods
                        //is inconsistent from one invocation to the next
                        //(simplifiedLeft should have been null 
                        //in the previous 'if' above).
                        throw new InvalidOperationException("Translation method is inconsistent: "+leftExprs);
                    }
                    if (rightExprs.Count == 0) {
                        //This can happen only when one of the create* methods
                        //is inconsistent from one invocation to the next
                        //(simplifiedRight should have been null 
                    	//in the previous 'if' above).
                        throw new InvalidOperationException("Translation method is inconsistent: "+rightExprs);
                    }
                                    
                    //Simulate ANDing each pair(left,right).
                    //If all of them return null (i.e., "everything"), 
                    //then the request cannot be filtered.
                    bool anyAndsPossible = false;
                    foreach ( T leftExpr in leftExprs ) {
                        foreach ( T rightExpr in rightExprs ) {
                            T test = CreateAndExpression(
                                    leftExpr,
                                    rightExpr);
                            if ( test != null ) {
                                anyAndsPossible = true;
                                break;
                            }
                        }
                        if ( anyAndsPossible ) {
                            break;
                        }
                    }
                   
                    //If no AND filtering is possible,
                    //return whichever of left or right 
                    //contains the fewest expressions.
                    if (!anyAndsPossible) {
                        if ( leftExprs.Count <= rightExprs.Count ) {
                            return simplifiedLeft;
                        }
                        else {
                            return simplifiedRight;
                        }
                    }
    
                    //Since AND filtering is possible for at least
                    //one expression, let's distribute.
                    if ( leftExprs.Count > 1 ) {
                        //The left can contain more than one expression
                    	//only if the left-hand side is an unimplemented OR.
                        //Distribute our AND to the left.
                        OrFilter left = (OrFilter)simplifiedLeft;
                        OrFilter newFilter =
                            new OrFilter(new AndFilter(left.Left,
                                                       simplifiedRight),
                                         new AndFilter(left.Right,
                                                       simplifiedRight));
                        return SimplifyAndDistribute(newFilter);
                    }
                    else if ( rightExprs.Count > 1 ) {
                        //The right can contain more than one expression 
                    	//only if the right-hand side is an unimplemented OR.
                        //Distribute our AND to the right.
                        OrFilter right = (OrFilter)simplifiedRight;
                        OrFilter newFilter =
                            new OrFilter(new AndFilter(simplifiedLeft,
                                right.Left),
                            new AndFilter(simplifiedLeft,
                                right.Right));
                        return SimplifyAndDistribute(newFilter);                    
                    }
                    else {
                        //Each side contains exactly one expression
                    	//and the translator does implement AND
                        //(anyAndsPossible must be true
                        //for them to have hit this branch).
                        if (!anyAndsPossible) {
                            throw new Exception("expected anyAndsPossible");
                        }
                        return new AndFilter(simplifiedLeft,simplifiedRight);                        
                    }
                }
            }
            else if ( filter is OrFilter ) {
                OrFilter of = (OrFilter)filter;
                Filter simplifiedLeft =
                    SimplifyAndDistribute(of.Left);
                Filter simplifiedRight =
                    SimplifyAndDistribute(of.Right);
                //If either left or right reduces to "everything", 
                //then simplify the OR to "everything".
                if ( simplifiedLeft == null || simplifiedRight == null ) {
                    return null;
                }
                //otherwise
                return new OrFilter(simplifiedLeft,
                        simplifiedRight);
            }
            else {
                //Otherwise, it's a NOT(LEAF) or a LEAF.
                //Simulate creating it.
                T expr = CreateLeafExpression(filter);
                if ( expr == null ) {
                    //If the expression cannot be implemented, 
                	//return the "everything" filter.
                    return null;
                }
                else {
                    //Otherwise, return the filter.
                    return filter;
                }
            }
        }
        
        /**
         * Translates the filter into a list of expressions. 
         * The filter must have already been transformed 
         * using normalizeNot followed by a simplifyAndDistribute.
         * @param filter A filter (normalized, simplified, and distibuted)
         * @return A list of expressions or empty list for everything.
         */
        private IList<T> TranslateInternal(Filter filter) {
            if ( filter is AndFilter ) {
                T result = TranslateAnd((AndFilter)filter);
                IList<T> rv = new List<T>();
                if ( result != null ) {
                    rv.Add(result);
                }
                return rv;
            }
            else if ( filter is OrFilter ) {
                return TranslateOr((OrFilter)filter);
            }
            else {
                //otherwise it's either a leaf or a NOT (leaf)
                T expr = CreateLeafExpression(filter);
                IList<T> exprs = new List<T>();
                if ( expr != null ) {
                    exprs.Add(expr);
                }
                return exprs;
            }
        }
        
        private T TranslateAnd( AndFilter filter ) {
            IList<T> leftExprs = TranslateInternal(filter.Left);
            IList<T> rightExprs = TranslateInternal(filter.Right);
            if ( leftExprs.Count != 1 ) {
                //this can happen only if one of the create* methods
                //is inconsistent from one invocation to the next
                //(at this point we've already been simplified and
                //distributed).
                throw new InvalidOperationException("Translation method is inconsistent: "+leftExprs);
            }
            if ( rightExprs.Count != 1 ) {
                //this can happen only if one of the create* methods
                //is inconsistent from one invocation to the next
                //(at this point we've already been simplified and
                //distributed).
                throw new InvalidOperationException("Translation method is inconsistent: "+rightExprs);
            }
            T rv = CreateAndExpression(leftExprs[0], rightExprs[0]);
            if ( rv == null ) {
                //This could happen only if we're inconsistent
                //(since the simplify logic already should have removed
                //any expression that cannot be filtered).
                throw new InvalidOperationException("createAndExpression is inconsistent");            
            }
            return rv;
        }
        
        private IList<T> TranslateOr( OrFilter filter ) {
            IList<T> leftExprs = TranslateInternal(filter.Left);
            IList<T> rightExprs = TranslateInternal(filter.Right);
            if ( leftExprs.Count == 0 ) {
                //This can happen only if one of the create* methods
                //is inconsistent from one invocation to the next.
                throw new InvalidOperationException("Translation method is inconsistent");            
            }
            if ( rightExprs.Count == 0 ) {
                //This can happen only if one of the create* methods
                //methods is inconsistent from on invocation to the next.
                throw new InvalidOperationException("Translation method is inconsistent");
            }
            if ( leftExprs.Count == 1 && rightExprs.Count == 1 ) {
                //If each side contains exactly one expression,
            	//try to create a combined expression.
            	T val = CreateOrExpression(leftExprs[0], rightExprs[0]);
                if ( val != null ) {
                    IList<T> rv = new List<T>();
                    rv.Add(val);
                    return rv;
                }
                //Otherwise, fall through
            }
            
            //Return a list of queries from the left and from the right
            IList<T> rv2 = new List<T>(leftExprs.Count+rightExprs.Count);
            CollectionUtil.AddAll(rv2,leftExprs);
            CollectionUtil.AddAll(rv2,rightExprs);
            return rv2;
        }
        
        /**
         * Creates an expression for a LEAF or a NOT(leaf)
         * @param filter Must be either a leaf or a NOT(leaf)
         * @return The expression
         */
        private T CreateLeafExpression(Filter filter) {
            Filter leafFilter;
            bool not;
            if ( filter is NotFilter ) {
                NotFilter nf = (NotFilter)filter;
                leafFilter = nf.Filter;
                not = true;
            }
            else {
                leafFilter = filter;
                not = false;
            }
            T expr = CreateLeafExpression(leafFilter,not);
            return expr;
        }
        
        /**
         * Creates a Leaf expression
         * @param filter Must be a leaf expression
         * @param not Is ! to be applied to the leaf expression
         * @return The expression or null (for everything)
         */
        private T CreateLeafExpression(Filter filter, bool not) {
            if ( filter is ContainsFilter ) {
                return CreateContainsExpression((ContainsFilter)filter, not);
            }
            else if (filter is EndsWithFilter) {
                return CreateEndsWithExpression((EndsWithFilter)filter,not);
            }
            else if ( filter is EqualsFilter ) {
                return CreateEqualsExpression((EqualsFilter)filter, not);
            }
            else if ( filter is GreaterThanFilter ) {
                return CreateGreaterThanExpression((GreaterThanFilter)filter, not);
            }
            else if ( filter is GreaterThanOrEqualFilter ) {
                return CreateGreaterThanOrEqualExpression((GreaterThanOrEqualFilter)filter, not);
            }
            else if ( filter is LessThanFilter ) {
                return CreateLessThanExpression((LessThanFilter)filter, not);
            }
            else if ( filter is LessThanOrEqualFilter ) {
                return CreateLessThanOrEqualExpression((LessThanOrEqualFilter)filter, not);
            }
            else if (filter is StartsWithFilter) {
                return CreateStartsWithExpression((StartsWithFilter)filter,not);
            }
            else if (filter is ContainsAllValuesFilter) {
                return CreateContainsAllValuesExpression((ContainsAllValuesFilter)filter, not);
            }
            else {
                //unrecognized expression - nothing we can do
                return null;
            }
        }
        
        /**
         * Should be overridden by subclasses to create an AND expression
         * if the native resource supports AND.
         * @param leftExpression The left expression. Will never be null.
         * @param rightExpression The right expression. Will never be null.
         * @return The AND expression. A return value of null means 
         * a native AND query cannot be created for the given expressions. 
         * In this case, the resulting query will consist of the 
         * leftExpression only.
         */
        protected virtual T CreateAndExpression(T leftExpression, T rightExpression) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create an OR expression
         * if the native resource supports OR.
         * @param leftExpression The left expression. Will never be null.
         * @param rightExpression The right expression. Will never be null.
         * @return The OR expression. A return value of null means 
         * a native OR query cannot be created for the given expressions. 
         * In this case, {@link #translate} may return multiple queries, each
         * of which must be run and results combined. 
         */
        protected virtual T CreateOrExpression(T leftExpression, T rightExpression) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a CONTAINS expression
         * if the native resource supports CONTAINS.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a  NOT CONTAINS
         * @return The CONTAINS expression. A return value of null means 
         * a native CONTAINS query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateContainsExpression(ContainsFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a ENDS-WITH expression
         * if the native resource supports ENDS-WITH.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT ENDS-WITH
         * @return The ENDS-WITH expression. A return value of null means 
         * a native ENDS-WITH query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateEndsWithExpression(EndsWithFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a EQUALS expression
         * if the native resource supports EQUALS.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT EQUALS
         * @return The EQUALS expression. A return value of null means 
         * a native EQUALS query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateEqualsExpression(EqualsFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a GREATER-THAN expression
         * if the native resource supports GREATER-THAN.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT GREATER-THAN
         * @return The GREATER-THAN expression. A return value of null means 
         * a native GREATER-THAN query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateGreaterThanExpression(GreaterThanFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a GREATER-THAN-EQUAL expression
         * if the native resource supports GREATER-THAN-EQUAL.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT GREATER-THAN-EQUAL
         * @return The GREATER-THAN-EQUAL expression. A return value of null means 
         * a native GREATER-THAN-EQUAL query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a LESS-THAN expression
         * if the native resource supports LESS-THAN.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT LESS-THAN
         * @return The LESS-THAN expression. A return value of null means 
         * a native LESS-THAN query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateLessThanExpression(LessThanFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a LESS-THAN-EQUAL expression
         * if the native resource supports LESS-THAN-EQUAL.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT LESS-THAN-EQUAL
         * @return The LESS-THAN-EQUAL expression. A return value of null means 
         * a native LESS-THAN-EQUAL query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateLessThanOrEqualExpression(LessThanOrEqualFilter filter, bool not) {
            return null;
        }
        
        /**
         * Should be overridden by subclasses to create a STARTS-WITH expression
         * if the native resource supports STARTS-WITH.
         * @param filter The contains filter. Will never be null.
         * @param not True if this should be a NOT STARTS-WITH
         * @return The STARTS-WITH expression. A return value of null means 
         * a native STARTS-WITH query cannot be created for the given filter. 
         * In this case, {@link #translate} may return an empty query set, meaning
         * fetch <b>everything</b>. The filter will be re-applied in memory
         * to the resulting object stream. This does not scale well, so
         * if possible, you should implement this method.
         */
        protected virtual T CreateStartsWithExpression(StartsWithFilter filter, bool not) {
            return null;
        }
        
        protected virtual T CreateContainsAllValuesExpression(ContainsAllValuesFilter filter, bool not) {
            return null;
        }
    }
    #endregion
    
    #region AndFilter
    public sealed class AndFilter : CompositeFilter {

        /**
         * And the the left and right filters.
         */
        public AndFilter(Filter left, Filter right) 
            : base(left, right) {
        }
    
        /**
         * Ands the left and right filters.
         * 
         * @see Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return Left.Accept(obj) && Right.Accept(obj);
        }
    
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("AND: ").Append(Left).Append(", ").Append(Right);
            return bld.ToString();
        }
    }
    #endregion
    
    #region AttributeFilter
    public abstract class AttributeFilter : Filter {
        private readonly ConnectorAttribute _attribute;
        
        /**
         * Root filter for Attribute testing..
         */
        internal AttributeFilter(ConnectorAttribute attribute) {
            _attribute = attribute;
            if (attribute == null) {
                throw new ArgumentException("Attribute not be null!");
            }
        }
        
        /**
         * Get the internal attribute.
         */
        public ConnectorAttribute GetAttribute() {
            return _attribute;
        }

        /**
         * Determines if the attribute provided is present in the
         * {@link ConnectorObject}.
         */
        public bool IsPresent(ConnectorObject obj) {
            return obj.GetAttributeByName(_attribute.Name) != null;
        }
        public abstract bool Accept(ConnectorObject obj);
    }
    #endregion

    #region ComparableAttributeFilter
    /**
     * Filter for an attribute value that is comparable.
     */
    public abstract class ComparableAttributeFilter :
            SingleValueAttributeFilter {
    
        /**
         * Attempt compare attribute values.
         */
        internal ComparableAttributeFilter(ConnectorAttribute attr) 
            : base(attr) {
            // determine if this attribute value is comparable..
            if (!(GetValue() is IComparable)) {
                String ERR = "Must be a comparable value!";
                throw new ArgumentException(ERR);
            }
        }
    
        /**
         * Call compareTo on the attribute values. If the attribute is not present
         * in the {@link ConnectorObject} return -1.
         */
        public int Compare(ConnectorObject obj) {
            int ret = -1;
            ConnectorAttribute attr = obj.GetAttributeByName(GetName());
            if (attr != null && attr.Value.Count == 1) {
                // it must be a comparable because that's were testing against
                if (!(attr.Value[0] is IComparable)) {
                    String ERR = "Attribute value must be comparable!";
                    throw new ArgumentException(ERR);
                }
                // grab this value and the on from the attribute an compare..
                IComparable o1 = (IComparable)attr.Value[0];
                IComparable o2 = (IComparable)GetValue();
                ret = o1.CompareTo(o1);
            }
            return ret;
        }
    }
    #endregion
    
    #region CompositeFilter
    public abstract class CompositeFilter : Filter {

        public Filter Left { get; set; }
    
        public Filter Right { get; set; }
    
        internal CompositeFilter(Filter left, Filter right) {
            Left = left;
            Right = right;
        }
        public abstract bool Accept(ConnectorObject obj);
    }
    #endregion

    #region ContainsFilter
    public sealed class ContainsFilter : StringFilter {

        public ContainsFilter(ConnectorAttribute attr) 
            : base(attr) {
        }
        
        public override bool Accept(String value) {
            return value.Contains(GetValue());
        }
        
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("CONTAINS: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion
    
    #region EndsWithFilter
    public sealed class EndsWithFilter : StringFilter {

        public EndsWithFilter(ConnectorAttribute attr) 
            : base(attr) {
        }
        
        public override bool Accept(String value) {
            return value.EndsWith(GetValue());
        }
        
        public override string ToString() {
            StringBuilder bld = new StringBuilder();
            bld.Append("ENDSWITH: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion

    #region EqualsFilter
    public sealed class EqualsFilter : AttributeFilter {

        /**
         * Determines if the attribute inside the {@link ConnectorObject} is equal
         * to the {@link Attribute} provided.
         */
        public EqualsFilter(ConnectorAttribute attr) 
            :base(attr) {
        }
    
        /**
         * Determines if the attribute exists in the {@link ConnectorObject} and if
         * its equal to the one provided.
         * 
         * @see Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            bool ret = false;
            ConnectorAttribute thisAttr = GetAttribute();
            ConnectorAttribute attr = obj.GetAttributeByName(thisAttr.Name);
            if (attr != null) {
                ret = thisAttr.Equals(attr);
            }
            return ret;
        }
        
        public override string ToString() {
            StringBuilder bld = new StringBuilder();
            bld.Append("EQUALS: ").Append(GetAttribute());
            return bld.ToString();
        }
    
    }
    #endregion

    #region Filter
    public interface Filter {
        bool Accept(ConnectorObject obj);
    }
    #endregion
    
    #region FilterBuilder
    /**
     * FilterBuilder creates a {@link Filter} object, that can determine if a
     * ConnectorObject will be filtered or not.
     * 
     * @author Will Droste
     * @version $Revision: 1.7 $
     * @since 1.0
     */
    public static class FilterBuilder {
    
        /**
         * Determine if the {@link ConnectorObject} {@link ConnectorAttribute} value ends
         * with the {@link ConnectorAttribute} value provided.
         * 
         * @param attr
         *            {@link ConnectorAttribute} value to test against the
         *            {@link ConnectorObject} attribute value.
         * @return true if the {@link ConnectorObject} attribute value contains the
         *         attribute value provided.
         */
        public static Filter EndsWith(ConnectorAttribute attr) {
            return new EndsWithFilter(attr);
        }
    
        /**
         * Determine if the {@link ConnectorObject} {@link ConnectorAttribute} value starts
         * with the {@link ConnectorAttribute} value provided.
         * 
         * @param attr
         *            {@link ConnectorAttribute} value to test against the
         *            {@link ConnectorObject} attribute value.
         * @return true if the {@link ConnectorObject} attribute value contains the
         *         attribute value provided.
         */
        public static Filter StartsWith(ConnectorAttribute attr) {
            return new StartsWithFilter(attr);
        }
    
        public static Filter ContainsAllValues(ConnectorAttribute attr) {
            return new ContainsAllValuesFilter(attr);
        }

        /**
         * Determine if the {@link ConnectorObject} {@link ConnectorAttribute} value contains
         * the {@link ConnectorAttribute} value provided.
         * 
         * @param attr
         *            {@link ConnectorAttribute} value to test against the
         *            {@link ConnectorObject} attribute value.
         * @return true if the {@link ConnectorObject} attribute value contains the
         *         attribute value provided.
         */
        public static Filter Contains(ConnectorAttribute attr) {
            return new ContainsFilter(attr);
        }
    
        /**
         * The {@link ConnectorAttribute} value provided is less than or equal to the
         * {@link ConnectorObject} attribute value.
         * 
         * @param attr
         *            ConnectorAttribute to do the comparison.
         * @return true if attribute provided is greater than or equal to the one
         *         provided by the {@link ConnectorObject}.
         */
        public static Filter GreaterThanOrEqualTo(ConnectorAttribute attr) {
            return new GreaterThanOrEqualFilter(attr);
        }
    
        /**
         * The {@link ConnectorAttribute} value provided is less than or equal to the
         * {@link ConnectorObject} attribute value.
         * 
         * @param attr
         *            ConnectorAttribute to do the comparison.
         * @return true if attribute provided is less than or equal to the one
         *         provided by the {@link ConnectorObject}.
         */
        public static Filter LessThanOrEqualTo(ConnectorAttribute attr) {
            return new LessThanOrEqualFilter(attr);
        }
    
        /**
         * The {@link ConnectorAttribute} value provided is less than the
         * {@link ConnectorObject} attribute value.
         * 
         * @param attr
         *            ConnectorAttribute to do the comparison.
         * @return true if attribute provided is less than the one provided by the
         *         {@link ConnectorObject}.
         */
        public static Filter LessThan(ConnectorAttribute attr) {
            return new LessThanFilter(attr);
        }
    
        /**
         * ConnectorAttribute value is greater than the {@link ConnectorObject} attribute
         * value.
         * 
         * @param attr
         *            ConnectorAttribute to do the comparison.
         * @return true if attribute provided is greater than the one provided by
         *         the {@link ConnectorObject}.
         */
        public static Filter GreaterThan(ConnectorAttribute attr) {
            return new GreaterThanFilter(attr);
        }
    
        /**
         * Determines if the {@link ConnectorAttribute} provided exists in the
         * {@link ConnectorObject} and is equal.
         */
        public static Filter EqualTo(ConnectorAttribute attr) {
            return new EqualsFilter(attr);
        }
    
        /**
         * Ands the two {@link Filter}.
         * 
         * @param leftOperand
         *            left side operand.
         * @param rightOperand
         *            right side operand.
         * @return the result of leftOperand &amp;&amp; rightOperand
         */
        public static Filter And(Filter leftOperand, Filter rightOperand) {
            return new AndFilter(leftOperand, rightOperand);
        }
    
        /**
         * ORs the two {@link Filter}.
         * 
         * @param leftOperand
         *            left side operand.
         * @param rightOperand
         *            right side operand.
         * @return the result of leftOperand || rightOperand
         */
        public static Filter Or(Filter leftOperand, Filter rightOperand) {
            return new OrFilter(leftOperand, rightOperand);
        }
        
        /**
         * NOT the {@link Filter}.
         * 
         * @param filter
         *            negate the result of {@link Filter}.
         * @return the result of not {@link Filter}.
         */
        public static Filter Not(Filter filter) {
            return new NotFilter(filter);
        }
    }
    #endregion
    
    #region FilterTranslator
    public interface FilterTranslator<T> {
       IList<T> Translate(Filter filter);
    }
    #endregion

    #region GreaterThanFilter
    public sealed class GreaterThanFilter : ComparableAttributeFilter {

        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         */
        public GreaterThanFilter(ConnectorAttribute attr) 
            : base (attr) {
        }
    
        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         * 
         * @see com.sun.openconnectors.framework.common.objects.Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return IsPresent(obj) && this.Compare(obj) > 0;
        }
    
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("GREATERTHAN: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion
    
    #region GreaterThanOrEqualFilter
    public sealed class GreaterThanOrEqualFilter : ComparableAttributeFilter {

        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         */
        public GreaterThanOrEqualFilter(ConnectorAttribute attr) 
            : base (attr) {
        }
    
        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         * 
         * @see com.sun.openconnectors.framework.common.objects.Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return IsPresent(obj) && this.Compare(obj) >= 0;
        }
    
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("GREATERTHANOREQUAL: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion

    #region LessThanFilter
    public sealed class LessThanFilter : ComparableAttributeFilter {

        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         */
        public LessThanFilter(ConnectorAttribute attr) 
            : base (attr) {
        }
    
        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         * 
         * @see com.sun.openconnectors.framework.common.objects.Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return IsPresent(obj) && this.Compare(obj) < 0;
        }

        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("LESSTHAN: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion

    #region LessThanOrEqualFilter
    public sealed class LessThanOrEqualFilter : ComparableAttributeFilter {

        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         */
        public LessThanOrEqualFilter(ConnectorAttribute attr) 
            : base (attr) {
        }
    
        /**
         * Determine if the {@link ConnectorObject} {@link Attribute} value is
         * greater than the one provided in the filter.
         * 
         * @see com.sun.openconnectors.framework.common.objects.Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return IsPresent(obj) && this.Compare(obj) <= 0;
        }

        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("LESSTHANOREQUAL: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion
    
    #region NotFilter
    /**
     * Proxy the filter to return the negative of the value.
     */
    public sealed class NotFilter : Filter {
    
        private readonly Filter _filter;
    
        /**
         * Take the value returned from the internal filter and NOT it.
         */
        public NotFilter(Filter filter) {
            _filter = filter;
        }
    
        /**
         * Get the internal filter that is being negated.
         */
        public Filter Filter {
            get {
                return _filter;
            }
        }
    
        /**
         * Return the opposite the internal filters return value.
         * 
         * @see Filter#accept(ConnectorObject)
         */
        public bool Accept(ConnectorObject obj) {
            return !_filter.Accept(obj);
        }
        
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("NOT: ").Append(Filter);
            return bld.ToString();
        }
    }
    #endregion
    
    #region OrFilter
    public sealed class OrFilter : CompositeFilter {

        /**
         * Or the left and right filters.
         */
        public OrFilter(Filter left, Filter right) 
            : base(left, right) {
        }
    
        /**
         * ORs the left and right filters.
         * 
         * @see Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            return Left.Accept(obj) || Right.Accept(obj);
        }

        public override string ToString() {
            StringBuilder bld = new StringBuilder();
            bld.Append("OR: ").Append(Left).Append(", ").Append(Right);
            return bld.ToString();
        }
    }
    #endregion
    
    #region SingleValueAttributeFilter
    /**
     * Get a single value out of the attribute to test w/.
     */
    public abstract class SingleValueAttributeFilter : AttributeFilter {
        
        /**
         * Attempt to single out the value for comparison.
         */
        internal SingleValueAttributeFilter(ConnectorAttribute attr) :
            base(attr) {
            // make sure this is not a Uid..
            if (Uid.NAME.Equals(attr.Name)) {
                String MSG = "Uid can only be used for equals comparison.";
                throw new ArgumentException(MSG);
            }
            // actual runtime..
            if (attr.Value.Count != 1) {
                String ERR = "Must only be one value!";
                throw new ArgumentException(ERR);
            }
        }
    
        /**
         * Value to test against.
         */
        public Object GetValue() {
            return GetAttribute().Value[0];
        }
        /**
         * Name of the attribute to find in the {@link ConnectorObject}.
         */
        public String GetName() {
            return GetAttribute().Name;
        }
    }
    #endregion
    
    #region StartsWithFilter
    public sealed class StartsWithFilter : StringFilter {

        public StartsWithFilter(ConnectorAttribute attr) 
            : base(attr) {
        }
        
        public override bool Accept(String value) {
            return value.StartsWith(GetValue());
        }
        
        public override string ToString()
        {
            StringBuilder bld = new StringBuilder();
            bld.Append("STARTSWITH: ").Append(GetAttribute());
            return bld.ToString();
        }
    }
    #endregion

    #region StringFilter
    /**
     * Filter based on strings.
     */
    public abstract class StringFilter : SingleValueAttributeFilter {
    
        /**
         * Attempts to get a string from the attribute.
         */
        internal StringFilter(ConnectorAttribute attr) 
            : base(attr) {
            Object val = base.GetValue();
            if (!(val is string)) {
                String MSG = "Value must be a string!";
                throw new ArgumentException(MSG);
            }
        }
    
        /**
         * Get the string value from the afore mentioned attribute.
         * 
         * @see SingleValueAttributeFilter#getValue()
         */
        public new String GetValue() {
            return (String) base.GetValue();
        }
        
        /**
         * @throws ClassCastException
         *             iff the value from the {@link ConnectorObject}'s attribute
         *             of the same name as provided is not a string.
         * @see com.sun.openconnectors.framework.common.objects.Filter#accept(ConnectorObject)
         */
        public override bool Accept(ConnectorObject obj) {
            bool ret = false;
            ConnectorAttribute attr = obj.GetAttributeByName(GetName());
            if (attr != null) {
                ret = Accept((string)attr.Value[0]);
            }
            return ret;
        }
        
        public abstract bool Accept(String value);
    }
    #endregion
    
    #region ContainsAllValues Filter
    public class ContainsAllValuesFilter : AttributeFilter {
        private readonly string _name;
        private readonly ICollection<object> _values;
        
        public ContainsAllValuesFilter(ConnectorAttribute attr) : base(attr) {
            _name = attr.Name;
            _values = attr.Value;
        }
        /**
         * Determine if the {@link ConnectorObject} contains an {@link Attribute}
         * which contains all the values provided in the {@link Attribute} passed
         * into the filter.
         * 
         * {@inheritDoc}
         */
        public override bool Accept(ConnectorObject obj) {
            bool rv = false;
            ConnectorAttribute found = obj.GetAttributeByName(_name);
            if (found != null) {
                // TODO: possible optimization using 'Set'
                foreach (object o in _values) {
                    if (!(rv = found.Value.Contains(o))) {
                        break;
                    }
                }
            }
            return rv;
        }
    }
    #endregion
    
}
