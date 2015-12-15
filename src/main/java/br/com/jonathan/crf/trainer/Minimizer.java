package br.com.jonathan.crf.trainer;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import br.com.jonathan.crf.function.CRFLogConditionalObjectiveFunction;
import br.com.jonathan.crf.utils.ArrayMath;

public class Minimizer{

	private int fevals = 0;
	private int maxFevals = -1;
	private int mem = 10;
	private int its = 0;
	private boolean quiet;
	private static final NumberFormat nf = new DecimalFormat( "0.000E0" );
	private static final NumberFormat nfsec = new DecimalFormat( "0.00" );
	private static final double ftol = 1e-4;
	private double gtol = 0.9;
	private static final double aMin = 1e-12;
	private static final double aMax = 1e12;
	private static final double p66 = 0.66;
	private static final double p5 = 0.5;
	private static final int a = 0;
	private static final int f = 1;
	private static final int g = 2;
	public boolean outputToFile = false;
	private boolean success = false;
	private boolean bracketed = false;
	private QNInfo presetInfo = null;
	private boolean noHistory = true;

	private boolean useOWLQN = false;
	private double lambdaOWL = 0;

	private boolean useAveImprovement = true;
	private boolean useRelativeNorm = true;
	private boolean useNumericalZero = true;
	private boolean useEvalImprovement = false;
	private boolean useMaxItr = false;
	private int maxItr = 0;

	private boolean suppressTestPrompt = false;
	private int terminateOnEvalImprovementNumOfEpoch = 1;

	private int evaluateIters = 0; // Evaluate every x iterations (0 = no evaluation)
	private int startEvaluateIters = 0; // starting evaluation after x iterations

	public Minimizer( int mem, boolean useEvalImprovement, int terminateOnEvalImprovementNumOfEpoch, int evaluateIters, int startEvaluateIters ){
		this.mem = mem;
		this.useEvalImprovement = useEvalImprovement;
		this.terminateOnEvalImprovementNumOfEpoch = terminateOnEvalImprovementNumOfEpoch;
		this.evaluateIters = evaluateIters;
		this.startEvaluateIters = startEvaluateIters;
	}

	private eLineSearch lsOpt = eLineSearch.MINPACK;
	private eScaling scaleOpt = eScaling.DIAGONAL;
	private eState state = eState.CONTINUE;

	public double[ ] minimize( CRFLogConditionalObjectiveFunction function, double functionTolerance, double[ ] initial, int maxFunctionEvaluations ) {
		System.err.println( "QNMinimizer called on double function of " + function.domainDimension() + " variables," );
		System.err.println( " using M = " + mem + "." );

		//init
		double[ ] x, newX, grad, newGrad, dir;
		double value;
		QNInfo qn = new QNInfo( mem );
		noHistory = true;
		its = 0;
		fevals = 0;
		success = false;
		qn.scaleOpt = scaleOpt;

		// initialize weights
		x = initial;

		// initialize gradient
		grad = new double[ x.length ];
		newGrad = new double[ x.length ];
		newX = new double[ x.length ];
		dir = new double[ x.length ];

		value = evaluateFunction( function, x, grad );

		Record rec = new Record( quiet, functionTolerance );
		rec.start( value, grad, x );

		maxFevals = ( maxFunctionEvaluations > 0 ) ? maxFunctionEvaluations : Integer.MAX_VALUE;

		System.err.println( "               An explanation of the output:" );
		System.err.println( "Iter           The number of iterations" );
		System.err.println( "evals          The number of function evaluations" );
		System.err.println( "SCALING        <D> Diagonal scaling was used; <I> Scaled Identity" );
		System.err.println( "LINESEARCH     [## M steplength]  Minpack linesearch" );
		System.err.println( "                   1-Function value was too high" );
		System.err.println( "                   2-Value ok, gradient positive, positive curvature" );
		System.err.println( "                   3-Value ok, gradient negative, positive curvature" );
		System.err.println( "                   4-Value ok, gradient negative, negative curvature" );
		System.err.println( "               [.. B]  Backtracking" );
		System.err.println( "VALUE          The current function value" );
		System.err.println( "TIME           Total elapsed time" );
		System.err.println( "|GNORM|        The current norm of the gradient" );
		System.err.println( "{RELNORM}      The ratio of the current to initial gradient norms" );
		System.err.println( "AVEIMPROVE     The average improvement / current value" );
		System.err.println( "EVALSCORE      The last available eval score" );
		System.err.println();
		System.err.println( "Iter ## evals ## <SCALING> [LINESEARCH] VALUE TIME |GNORM| {RELNORM} AVEIMPROVE EVALSCORE" );

		do {
			try {
				boolean doEval = ( its >= 0 && its >= startEvaluateIters && evaluateIters > 0 && its % evaluateIters == 0 );
				its += 1;
				double newValue;
				double[ ] newPoint = new double[ 3 ];
				System.err.print( "Iter " + its + " evals " + fevals + " " );

				System.err.print( "<" );
				computeDir( dir, grad, x, qn, function );
				System.err.print( "> " );

				boolean hasNaNDir = false;
				boolean hasNaNGrad = false;
				for ( int i = 0; i < dir.length; i++ ) {
					if ( dir[ i ] != dir[ i ] )
						hasNaNDir = true;
					if ( grad[ i ] != grad[ i ] )
						hasNaNGrad = true;
				}
				if ( hasNaNDir && !hasNaNGrad ) {
					System.err.println( "(NaN dir likely due to Hessian approx - resetting) " );
					qn.clear();
					// re-compute the search direction
					System.err.print( "<" );
					computeDir( dir, grad, x, qn, function );
					System.err.print( "> " );
				}

				System.err.print( "[" );

				switch ( lsOpt ) {
					case BACKTRACK:
						newPoint = lineSearchBacktrack( function, dir, x, newX, grad, value );
						System.err.print( "B" );
						break;
					case MINPACK:
						newPoint = lineSearchMinPack( function, dir, x, newX, grad, value, functionTolerance );
						System.err.print( "M" );
						break;
					default:
						throw new IllegalArgumentException( "Invalid line search option for QNMinimizer." );
				}

				newValue = newPoint[ f ];
				System.err.print( " " );
				System.err.print( nf.format( newPoint[ a ] ) );
				System.err.print( "] " );

				System.arraycopy( function.derivativeAt( newX ), 0, newGrad, 0, newGrad.length );

				qn.update( newX, x, newGrad, grad, newPoint[ a ] ); // step (4) in Galen & Gao 2007

				double evalScore = Double.NEGATIVE_INFINITY;
				if ( doEval ) {
					evalScore = doEvaluation( newX );
				}

				rec.add( newValue, newGrad, newX, fevals, evalScore );

				value = newValue;

				System.arraycopy( newX, 0, x, 0, x.length );
				System.arraycopy( newGrad, 0, grad, 0, newGrad.length );

				if ( quiet ) {
					System.err.print( "." );
				}

				if ( fevals > maxFevals ) {
					throw new MaxEvaluationsExceeded( " Exceeded in minimize() loop " );
				}
			} catch ( SurpriseConvergence s ) {
				System.err.println();
				System.err.println( "QNMinimizer aborted due to surprise convergence" );
				break;
			} catch ( MaxEvaluationsExceeded m ) {
				System.err.println();
				System.err.println( "QNMinimizer aborted due to maximum number of function evaluations" );
				System.err.println( m.toString() );
				System.err.println( "** This is not an acceptable termination of QNMinimizer, consider" );
				System.err.println( "** increasing the max number of evaluations, or safeguarding your" );
				System.err.println( "** program by checking the QNMinimizer.wasSuccessful() method." );
				break;
			}

		} while ( ( state = rec.toContinue() ) == eState.CONTINUE );

		if ( evaluateIters > 0 ) {
			double evalScore = ( useEvalImprovement ? doEvaluation( rec.getBest() ) : doEvaluation( x ) );
			System.err.println( "final evalScore is: " + evalScore );
		}

		System.err.println();

		switch ( state ) {
			case TERMINATE_GRADNORM:
				System.err.println( "QNMinimizer terminated due to numerically zero gradient: |g| < EPS  max(1,|x|) " );
				success = true;
				break;
			case TERMINATE_RELATIVENORM:
				System.err.println( "QNMinimizer terminated due to sufficient decrease in gradient norms: |g|/|g0| < TOL " );
				success = true;
				break;
			case TERMINATE_AVERAGEIMPROVE:
				System.err.println( "QNMinimizer terminated due to average improvement: | newest_val - previous_val | / |newestVal| < TOL " );
				success = true;
				break;
			case TERMINATE_MAXITR:
				System.err.println( "QNMinimizer terminated due to reached max iteration " + maxItr );
				success = true;
				break;
			case TERMINATE_EVALIMPROVE:
				System.err.println( "QNMinimizer terminated due to no improvement on eval " );
				success = true;
				x = rec.getBest();
				break;
			default:
				System.err.println( "QNMinimizer terminated without converging" );
				success = false;
				break;
		}

		double completionTime = rec.howLong();
		System.err.println( "Total time spent in optimization: " + nfsec.format( completionTime ) + "s" );

		qn.free();

		return x;
	}

	private double doEvaluation( double[ ] x ) {
		return Double.NEGATIVE_INFINITY;
	}

	private double[ ] lineSearchMinPack( CRFLogConditionalObjectiveFunction dfunc, double[ ] dir, double[ ] x, double[ ] newX, double[ ] grad, double f0, double tol ) throws MaxEvaluationsExceeded {
		double xtrapf = 4.0;
		int info = 0;
		int infoc = 1;
		bracketed = false;
		boolean stage1 = true;
		double width = aMax - aMin;
		double width1 = 2 * width;

		double g0 = ArrayMath.innerProduct( grad, dir );
		if ( g0 >= 0 ) {
			for ( int i = 0; i < x.length; i++ ) {
				dir[ i ] = -grad[ i ];
			}
			g0 = ArrayMath.innerProduct( grad, dir );
		}
		double gTest = ftol * g0;

		double[ ] newPt = new double[ 3 ];
		double[ ] bestPt = new double[ 3 ];
		double[ ] endPt = new double[ 3 ];

		newPt[ a ] = 1.0;

		if ( its == 1 && noHistory ) {
			newPt[ a ] = 1e-1;
		}

		bestPt[ a ] = 0.0;
		bestPt[ f ] = f0;
		bestPt[ g ] = g0;
		endPt[ a ] = 0.0;
		endPt[ f ] = f0;
		endPt[ g ] = g0;

		do {
			double stpMin;
			double stpMax;

			if ( bracketed ) {
				stpMin = Math.min( bestPt[ a ], endPt[ a ] );
				stpMax = Math.max( bestPt[ a ], endPt[ a ] );
			} else {
				stpMin = bestPt[ a ];
				stpMax = newPt[ a ] + xtrapf * ( newPt[ a ] - bestPt[ a ] );
			}

			newPt[ a ] = Math.max( newPt[ a ], aMin );
			newPt[ a ] = Math.min( newPt[ a ], aMax );

			if ( ( bracketed && ( newPt[ a ] <= stpMin || newPt[ a ] >= stpMax ) ) || fevals >= maxFevals || infoc == 0 || ( bracketed && stpMax - stpMin <= tol * stpMax ) ) {
				// todo: below..
				plusAndConstMult( x, dir, bestPt[ a ], newX );
				newPt[ f ] = bestPt[ f ];
				newPt[ a ] = bestPt[ a ];
			}

			newPt[ f ] = dfunc.valueAt( ( plusAndConstMult( x, dir, newPt[ a ], newX ) ) );
			newPt[ g ] = ArrayMath.innerProduct( dfunc.derivativeAt( newX ), dir );
			double fTest = f0 + newPt[ a ] * gTest;
			fevals += 1;

			if ( ( bracketed && ( newPt[ a ] <= stpMin || newPt[ a ] >= stpMax ) ) || infoc == 0 ) {
				info = 6;
				System.err.print( " line search failure: bracketed but no feasible found " );
			}
			if ( newPt[ a ] == aMax && newPt[ f ] <= fTest && newPt[ g ] <= gTest ) {
				info = 5;
				System.err.print( " line search failure: sufficient decrease, but gradient is more negative " );
			}
			if ( newPt[ a ] == aMin && ( newPt[ f ] > fTest || newPt[ g ] >= gTest ) ) {
				info = 4;
				System.err.print( " line search failure: minimum step length reached " );
			}
			if ( fevals >= maxFevals ) {
				info = 3;
				throw new MaxEvaluationsExceeded( " Exceeded during linesearch() Function " );
			}
			if ( bracketed && stpMax - stpMin <= tol * stpMax ) {
				info = 2;
				System.err.print( " line search failure: interval is too small " );
			}
			if ( newPt[ f ] <= fTest && Math.abs( newPt[ g ] ) <= -gtol * g0 ) {
				info = 1;
			}

			if ( info != 0 ) {
				return newPt;
			}

			if ( stage1 && newPt[ f ] <= fTest && newPt[ g ] >= Math.min( ftol, gtol ) * g0 ) {
				stage1 = false;
			}

			if ( stage1 && newPt[ f ] <= bestPt[ f ] && newPt[ f ] > fTest ) {
				newPt[ f ] = newPt[ f ] - newPt[ a ] * gTest;
				bestPt[ f ] = bestPt[ f ] - bestPt[ a ] * gTest;
				endPt[ f ] = endPt[ f ] - endPt[ a ] * gTest;

				newPt[ g ] = newPt[ g ] - gTest;
				bestPt[ g ] = bestPt[ g ] - gTest;
				endPt[ g ] = endPt[ g ] - gTest;

				infoc = getStep( /* x, dir, newX, f0, g0, */
				newPt, bestPt, endPt, stpMin, stpMax );

				bestPt[ f ] = bestPt[ f ] + bestPt[ a ] * gTest;
				endPt[ f ] = endPt[ f ] + endPt[ a ] * gTest;

				bestPt[ g ] = bestPt[ g ] + gTest;
				endPt[ g ] = endPt[ g ] + gTest;
			} else {
				infoc = getStep( /* x, dir, newX, f0, g0, */
				newPt, bestPt, endPt, stpMin, stpMax );
			}

			if ( bracketed ) {
				if ( Math.abs( endPt[ a ] - bestPt[ a ] ) >= p66 * width1 ) {
					newPt[ a ] = bestPt[ a ] + p5 * ( endPt[ a ] - bestPt[ a ] );
				}
				width1 = width;
				width = Math.abs( endPt[ a ] - bestPt[ a ] );
			}

		} while ( true );

	}

	private int getStep(
	        /* double[] x, double[] dir, double[] newX, double f0,
	        double g0, // None of these were used */
	        double[ ] newPt, double[ ] bestPt, double[ ] endPt, double stpMin, double stpMax ) {
		int info; // = 0; always set in the if below
		boolean bound; // = false; always set in the if below
		double theta, gamma, p, q, r, s, stpc, stpq, stpf;
		double signG = newPt[ g ] * bestPt[ g ] / Math.abs( bestPt[ g ] );

		if ( newPt[ f ] > bestPt[ f ] ) {
			info = 1;
			bound = true;
			theta = 3 * ( bestPt[ f ] - newPt[ f ] ) / ( newPt[ a ] - bestPt[ a ] ) + bestPt[ g ] + newPt[ g ];
			s = Math.max( Math.max( theta, newPt[ g ] ), bestPt[ g ] );
			gamma = s * Math.sqrt( ( theta / s ) * ( theta / s ) - ( bestPt[ g ] / s ) * ( newPt[ g ] / s ) );
			if ( newPt[ a ] < bestPt[ a ] ) {
				gamma = -gamma;
			}
			p = ( gamma - bestPt[ g ] ) + theta;
			q = ( ( gamma - bestPt[ g ] ) + gamma ) + newPt[ g ];
			r = p / q;
			stpc = bestPt[ a ] + r * ( newPt[ a ] - bestPt[ a ] );
			stpq = bestPt[ a ] + ( ( bestPt[ g ] / ( ( bestPt[ f ] - newPt[ f ] ) / ( newPt[ a ] - bestPt[ a ] ) + bestPt[ g ] ) ) / 2 ) * ( newPt[ a ] - bestPt[ a ] );

			if ( Math.abs( stpc - bestPt[ a ] ) < Math.abs( stpq - bestPt[ a ] ) ) {
				stpf = stpc;
			} else {
				stpf = stpq;
				// stpf = stpc + (stpq - stpc)/2;
			}
			bracketed = true;
			if ( newPt[ a ] < 0.1 ) {
				stpf = 0.01 * stpf;
			}

		} else if ( signG < 0.0 ) {
			//
			// Second case. A lower function value and derivatives of
			// opposite sign. The minimum is bracketed. If the cubic
			// step is closer to stx than the quadratic (secant) step,
			// the cubic step is taken, else the quadratic step is taken.
			//
			info = 2;
			bound = false;
			theta = 3 * ( bestPt[ f ] - newPt[ f ] ) / ( newPt[ a ] - bestPt[ a ] ) + bestPt[ g ] + newPt[ g ];
			s = Math.max( Math.max( theta, bestPt[ g ] ), newPt[ g ] );
			gamma = s * Math.sqrt( ( theta / s ) * ( theta / s ) - ( bestPt[ g ] / s ) * ( newPt[ g ] / s ) );
			if ( newPt[ a ] > bestPt[ a ] ) {
				gamma = -gamma;
			}
			p = ( gamma - newPt[ g ] ) + theta;
			q = ( ( gamma - newPt[ g ] ) + gamma ) + bestPt[ g ];
			r = p / q;
			stpc = newPt[ a ] + r * ( bestPt[ a ] - newPt[ a ] );
			stpq = newPt[ a ] + ( newPt[ g ] / ( newPt[ g ] - bestPt[ g ] ) ) * ( bestPt[ a ] - newPt[ a ] );
			if ( Math.abs( stpc - newPt[ a ] ) > Math.abs( stpq - newPt[ a ] ) ) {
				stpf = stpc;
			} else {
				stpf = stpq;
			}
			bracketed = true;

		} else if ( Math.abs( newPt[ g ] ) < Math.abs( bestPt[ g ] ) ) {
			//
			// Third case. A lower function value, derivatives of the
			// same sign, and the magnitude of the derivative decreases.
			// The cubic step is only used if the cubic tends to infinity
			// in the direction of the step or if the minimum of the cubic
			// is beyond stp. Otherwise the cubic step is defined to be
			// either stpmin or stpmax. The quadratic (secant) step is also
			// computed and if the minimum is bracketed then the the step
			// closest to stx is taken, else the step farthest away is taken.
			//
			info = 3;
			bound = true;
			theta = 3 * ( bestPt[ f ] - newPt[ f ] ) / ( newPt[ a ] - bestPt[ a ] ) + bestPt[ g ] + newPt[ g ];
			s = Math.max( Math.max( theta, bestPt[ g ] ), newPt[ g ] );
			gamma = s * Math.sqrt( Math.max( 0.0, ( theta / s ) * ( theta / s ) - ( bestPt[ g ] / s ) * ( newPt[ g ] / s ) ) );
			if ( newPt[ a ] < bestPt[ a ] ) {
				gamma = -gamma;
			}
			p = ( gamma - bestPt[ g ] ) + theta;
			q = ( ( gamma - bestPt[ g ] ) + gamma ) + newPt[ g ];
			r = p / q;
			if ( r < 0.0 && gamma != 0.0 ) {
				stpc = newPt[ a ] + r * ( bestPt[ a ] - newPt[ a ] );
			} else if ( newPt[ a ] > bestPt[ a ] ) {
				stpc = stpMax;
			} else {
				stpc = stpMin;
			}
			stpq = newPt[ a ] + ( newPt[ g ] / ( newPt[ g ] - bestPt[ g ] ) ) * ( bestPt[ a ] - newPt[ a ] );

			if ( bracketed ) {
				if ( Math.abs( newPt[ a ] - stpc ) < Math.abs( newPt[ a ] - stpq ) ) {
					stpf = stpc;
				} else {
					stpf = stpq;
				}
			} else {
				if ( Math.abs( newPt[ a ] - stpc ) > Math.abs( newPt[ a ] - stpq ) ) {
					stpf = stpc;
				} else {
					stpf = stpq;
				}
			}

		} else {
			//
			// Fourth case. A lower function value, derivatives of the
			// same sign, and the magnitude of the derivative does
			// not decrease. If the minimum is not bracketed, the step
			// is either stpmin or stpmax, else the cubic step is taken.
			//
			info = 4;
			bound = false;

			if ( bracketed ) {
				theta = 3 * ( bestPt[ f ] - newPt[ f ] ) / ( newPt[ a ] - bestPt[ a ] ) + bestPt[ g ] + newPt[ g ];
				s = Math.max( Math.max( theta, bestPt[ g ] ), newPt[ g ] );
				gamma = s * Math.sqrt( ( theta / s ) * ( theta / s ) - ( bestPt[ g ] / s ) * ( newPt[ g ] / s ) );
				if ( newPt[ a ] > bestPt[ a ] ) {
					gamma = -gamma;
				}
				p = ( gamma - newPt[ g ] ) + theta;
				q = ( ( gamma - newPt[ g ] ) + gamma ) + bestPt[ g ];
				r = p / q;
				stpc = newPt[ a ] + r * ( bestPt[ a ] - newPt[ a ] );
				stpf = stpc;
			} else if ( newPt[ a ] > bestPt[ a ] ) {
				stpf = stpMax;
			} else {
				stpf = stpMin;
			}

		}

		//
		// Update the interval of uncertainty. This update does not
		// depend on the new step or the case analysis above.
		//
		if ( newPt[ f ] > bestPt[ f ] ) {
			copy( newPt, endPt );
		} else {
			if ( signG < 0.0 ) {
				copy( bestPt, endPt );
			}
			copy( newPt, bestPt );
		}

		System.err.print( String.valueOf( info ) );

		//
		// Compute the new step and safeguard it.
		//
		stpf = Math.min( stpMax, stpf );
		stpf = Math.max( stpMin, stpf );
		newPt[ a ] = stpf;

		if ( bracketed && bound ) {
			if ( endPt[ a ] > bestPt[ a ] ) {
				newPt[ a ] = Math.min( bestPt[ a ] + p66 * ( endPt[ a ] - bestPt[ a ] ), newPt[ a ] );
			} else {
				newPt[ a ] = Math.max( bestPt[ a ] + p66 * ( endPt[ a ] - bestPt[ a ] ), newPt[ a ] );
			}
		}

		return info;
	}

	private static void copy( double[ ] src, double[ ] dest ) {
		System.arraycopy( src, 0, dest, 0, src.length );
	}

	private double[ ] lineSearchBacktrack( CRFLogConditionalObjectiveFunction func, double[ ] dir, double[ ] x, double[ ] newX, double[ ] grad, double lastValue ) throws MaxEvaluationsExceeded {
		double normGradInDir = ArrayMath.innerProduct( dir, grad );
		System.err.print( "(" + nf.format( normGradInDir ) + ")" );
		if ( normGradInDir > 0 ) {
			System.err.println( "{WARNING--- direction of positive gradient chosen!}" );
		}

		double step, c1;
		if ( its <= 2 ) {
			step = 0.1;
			c1 = 0.1;
		} else {
			step = 1.0;
			c1 = 0.1;
		}

		double c = 0.01;
		c = c * normGradInDir;
		double[ ] newPoint = new double[ 3 ];

		while ( ( newPoint[ f ] = func.valueAt( ( plusAndConstMult( x, dir, step, newX ) ) ) ) > lastValue + c * step ) {
			fevals += 1;
			if ( newPoint[ f ] < lastValue ) {
				System.err.print( "!" );
			} else {
				System.err.print( "." );
			}
			step = c1 * step;
		}

		newPoint[ a ] = step;
		fevals += 1;
		if ( fevals > maxFevals ) {
			throw new MaxEvaluationsExceeded( " Exceeded during linesearch() Function " );
		}

		return newPoint;

	}

	private void computeDir( double[ ] dir, double[ ] fg, double[ ] x, QNInfo qn, CRFLogConditionalObjectiveFunction function ) throws SurpriseConvergence {
		System.arraycopy( fg, 0, dir, 0, fg.length );

		int mmm = qn.size();
		double[ ] as = new double[ mmm ];

		for ( int i = mmm - 1; i >= 0; i-- ) {
			as[ i ] = qn.getRho( i ) * ArrayMath.innerProduct( qn.getS( i ), dir );
			plusAndConstMult( dir, qn.getY( i ), -as[ i ], dir );
		}

		qn.applyInitialHessian( dir );

		for ( int i = 0; i < mmm; i++ ) {
			double b = qn.getRho( i ) * ArrayMath.innerProduct( qn.getY( i ), dir );
			plusAndConstMult( dir, qn.getS( i ), as[ i ] - b, dir );
		}

		ArrayMath.multiplyInPlace( dir, -1 );
	}

	private static double[ ] plusAndConstMult( double[ ] a, double[ ] b, double c, double[ ] d ) {
		for ( int i = 0; i < a.length; i++ ) {
			d[ i ] = a[ i ] + c * b[ i ];
		}
		return d;
	}

	private double evaluateFunction( CRFLogConditionalObjectiveFunction dfunc, double[ ] x, double[ ] grad ) {
		System.arraycopy( dfunc.derivativeAt( x ), 0, grad, 0, grad.length );
		fevals += 1;
		return dfunc.valueAt( x );
	}

	//TODO Class QNInfo
	public class QNInfo{
		// Diagonal Options
		// Linesearch Options
		// Memory stuff
		private List< double[ ] > s = null;
		private List< double[ ] > y = null;
		private List< Double > rho = null;
		private double gamma;
		public double[ ] d = null;
		private int mem;
		private int maxMem = 20;
		public eScaling scaleOpt = eScaling.SCALAR;

		public QNInfo( int size ){
			s = new ArrayList< double[ ] >();
			y = new ArrayList< double[ ] >();
			rho = new ArrayList< Double >();
			gamma = 1;
			mem = size;
		}

		public QNInfo(){
			s = new ArrayList< double[ ] >();
			y = new ArrayList< double[ ] >();
			rho = new ArrayList< Double >();
			gamma = 1;
			mem = maxMem;
		}

		public QNInfo( List< double[ ] > sList, List< double[ ] > yList ){
			s = new ArrayList< double[ ] >();
			y = new ArrayList< double[ ] >();
			rho = new ArrayList< Double >();
			gamma = 1;
			setHistory( sList, yList );
		}

		public int size() {
			return s.size();
		}

		public double getRho( int ind ) {
			return rho.get( ind );
		}

		public double[ ] getS( int ind ) {
			return s.get( ind );
		}

		public double[ ] getY( int ind ) {
			return y.get( ind );
		}

		public void useDiagonalScaling() {
			this.scaleOpt = eScaling.DIAGONAL;
		}

		public void useScalarScaling() {
			this.scaleOpt = eScaling.SCALAR;
		}

		/*
		 * Free up that memory.
		 */
		public void free() {
			s = null;
			y = null;
			rho = null;
			d = null;
		}

		public void clear() {
			s.clear();
			y.clear();
			rho.clear();
			d = null;
		}

		/*
		 * applyInitialHessian(double[] x)
		 *
		 * This function takes the vector x, and applies the best guess at the
		 * initial hessian to this vector, based off available information from
		 * previous updates.
		 */
		public void setHistory( List< double[ ] > sList, List< double[ ] > yList ) {
			int size = sList.size();

			for ( int i = 0; i < size; i++ ) {
				update( sList.get( i ), yList.get( i ), ArrayMath.innerProduct( yList.get( i ), yList.get( i ) ), ArrayMath.innerProduct( sList.get( i ), yList.get( i ) ), 0, 1.0 );
			}
		}

		public double[ ] applyInitialHessian( double[ ] x ) {

			switch ( scaleOpt ) {
				case SCALAR:
					System.err.print( "I" );
					ArrayMath.multiplyInPlace( x, gamma );
					break;
				case DIAGONAL:
					System.err.print( "D" );
					if ( d != null ) {
						// Check sizes
						if ( x.length != d.length ) {
							throw new IllegalArgumentException( "Vector of incorrect size passed to applyInitialHessian in QNInfo class" );
						}
						// Scale element-wise
						for ( int i = 0; i < x.length; i++ ) {
							x[ i ] = x[ i ] / ( d[ i ] );
						}
					}
					break;
			}

			return x;

		}

		/*
		 * The update function is used to update the hessian approximation used by
		 * the quasi newton optimization routine.
		 *
		 * If everything has behaved nicely, this involves deciding on a new initial
		 * hessian through scaling or diagonal update, and then storing of the
		 * secant pairs s = x - previousX and y = grad - previousGrad.
		 *
		 * Things can go wrong, if any non convex behavior is detected (s^T y &lt; 0)
		 * or numerical errors are likely the update is skipped.
		 *
		 */
		public int update( double[ ] newX, double[ ] x, double[ ] newGrad, double[ ] grad, double step ) throws SurpriseConvergence {
			// todo: add outofmemory error.
			double[ ] newS, newY;
			double sy, yy, sg;

			// allocate arrays for new s,y pairs (or replace if the list is already
			// full)
			if ( mem > 0 && s.size() == mem || s.size() == maxMem ) {
				newS = s.remove( 0 );
				newY = y.remove( 0 );
				rho.remove( 0 );
			} else {
				newS = new double[ x.length ];
				newY = new double[ x.length ];
			}

			// Here we construct the new pairs, and check for positive definiteness.
			sy = 0;
			yy = 0;
			sg = 0;
			for ( int i = 0; i < x.length; i++ ) {
				newS[ i ] = newX[ i ] - x[ i ];
				newY[ i ] = newGrad[ i ] - grad[ i ];
				sy += newS[ i ] * newY[ i ];
				yy += newY[ i ] * newY[ i ];
				sg += newS[ i ] * newGrad[ i ];
			}

			// Apply the updates used for the initial hessian.

			return update( newS, newY, yy, sy, sg, step );
		}

		private class NegativeCurvature extends Throwable{
			/**
			 *
			 */
			private static final long serialVersionUID = 4676562552506850519L;

			public NegativeCurvature(){}
		}

		private class ZeroGradient extends Throwable{
			/**
			 *
			 */
			private static final long serialVersionUID = -4001834044987928521L;

			public ZeroGradient(){}
		}

		public int update( double[ ] newS, double[ ] newY, double yy, double sy, double sg, double step ) {

			// Initialize diagonal to the identity
			if ( scaleOpt == eScaling.DIAGONAL && d == null ) {
				d = new double[ newS.length ];
				for ( int i = 0; i < d.length; i++ ) {
					d[ i ] = 1.0;
				}
			}

			try {

				if ( sy < 0 ) {
					throw new NegativeCurvature();
				}

				if ( yy == 0.0 ) {
					throw new ZeroGradient();
				}

				switch ( scaleOpt ) {
					/*
					 * SCALAR: The standard L-BFGS initial approximation which is just a
					 * scaled identity.
					 */
					case SCALAR:
						gamma = sy / yy;
						break;
					/*
					 * DIAGONAL: A diagonal scaling matrix is used as the initial
					 * approximation. The updating method used is used thanks to Andrew
					 * Bradley of the ICME dept.
					 */
					case DIAGONAL:

						double sDs;
						// Gamma is designed to scale such that a step length of one is
						// generally accepted.
						gamma = sy / ( step * ( sy - sg ) );
						sDs = 0.0;
						for ( int i = 0; i < d.length; i++ ) {
							d[ i ] = gamma * d[ i ];
							sDs += newS[ i ] * d[ i ] * newS[ i ];
						}
						// This diagonal update was introduced by Andrew Bradley
						for ( int i = 0; i < d.length; i++ ) {
							d[ i ] = ( 1 - d[ i ] * newS[ i ] * newS[ i ] / sDs ) * d[ i ] + newY[ i ] * newY[ i ] / sy;
						}
						// Here we make sure that the diagonal is alright
						double minD = ArrayMath.min( d );
						double maxD = ArrayMath.max( d );

						// If things have gone bad, just fill with the SCALAR approx.
						if ( minD <= 0 || Double.isInfinite( maxD ) || maxD / minD > 1e12 ) {
							System.err.println( "QNInfo:update() : PROBLEM WITH DIAGONAL UPDATE" );
							double fill = yy / sy;
							for ( int i = 0; i < d.length; i++ ) {
								d[ i ] = fill;
							}
						}

				}

				// If s is already of size mem, remove the oldest vector and free it up.

				if ( mem > 0 && s.size() == mem || s.size() == maxMem ) {
					s.remove( 0 );
					y.remove( 0 );
					rho.remove( 0 );
				}

				// Actually add the pair.
				s.add( newS );
				y.add( newY );
				rho.add( 1 / sy );

			} catch ( NegativeCurvature nc ) {
				// NOTE: if applying QNMinimizer to a non convex problem, we would still
				// like to update the matrix
				// or we could get stuck in a series of skipped updates.
				System.err.println( " Negative curvature detected, update skipped " );
			} catch ( ZeroGradient zg ) {
				System.err.println( " Either convergence, or floating point errors combined with extremely linear region " );
			}

			return s.size();
		} // end update

	} // end class QNInfo

	//TODO Enum scala
	public enum eScaling {
		DIAGONAL, SCALAR
	}

	//TODO Enum estado
	public enum eState {
		TERMINATE_MAXEVALS, TERMINATE_RELATIVENORM, TERMINATE_GRADNORM, TERMINATE_AVERAGEIMPROVE, CONTINUE, TERMINATE_EVALIMPROVE, TERMINATE_MAXITR
	}

	//TODO Enum tipo de busca
	public enum eLineSearch {
		BACKTRACK, MINPACK
	}

	//TODO Class Record
	public class Record{

		private final List< Double > evals = new ArrayList< Double >();
		private final List< Double > values = new ArrayList< Double >();
		List< Double > gNorms = new ArrayList< Double >();
		private final List< Integer > funcEvals = new ArrayList< Integer >();
		private final List< Double > time = new ArrayList< Double >();
		private double gNormInit = Double.MIN_VALUE;
		private double relativeTOL = 1e-8;
		private double TOL = 1e-6;
		private double EPS = 1e-6;
		private long startTime;
		private double gNormLast;
		private double[ ] xLast;
		private int maxSize = 100;
		private boolean quiet = false;
		private boolean memoryConscious = true;

		private int noImproveItrCount = 0;
		private double[ ] xBest;

		public Record( boolean beQuiet, double tolerance ){
			this.quiet = beQuiet;
			this.TOL = tolerance;
		}

		public eState toContinue() {
			double relNorm = gNormLast / gNormInit;
			int size = values.size();
			double newestVal = values.get( size - 1 );
			double previousVal = ( size >= 10 ? values.get( size - 10 ) : values.get( 0 ) );
			double averageImprovement = ( previousVal - newestVal ) / ( size >= 10 ? 10 : size );
			int evalsSize = evals.size();

			if ( useMaxItr && its >= maxItr )
				return eState.TERMINATE_MAXITR;

			if ( useEvalImprovement ) {
				int bestInd = -1;
				double bestScore = Double.NEGATIVE_INFINITY;
				for ( int i = 0; i < evalsSize; i++ ) {
					if ( evals.get( i ) >= bestScore ) {
						bestScore = evals.get( i );
						bestInd = i;
					}
				}
				if ( bestInd == evalsSize - 1 ) { // copy xBest
					if ( xBest == null )
						xBest = Arrays.copyOf( xLast, xLast.length );
					else
						System.arraycopy( xLast, 0, xBest, 0, xLast.length );
				}
				if ( ( evalsSize - bestInd ) >= terminateOnEvalImprovementNumOfEpoch )
					return eState.TERMINATE_EVALIMPROVE;
			}

			if ( useAveImprovement && ( size > 5 && Math.abs( averageImprovement / newestVal ) < TOL ) ) {
				return eState.TERMINATE_AVERAGEIMPROVE;
			}

			if ( useRelativeNorm && relNorm <= relativeTOL ) {
				return eState.TERMINATE_RELATIVENORM;
			}

			if ( useNumericalZero ) {
				if ( gNormLast < EPS * Math.max( 1.0, ArrayMath.norm_1( xLast ) ) ) {
					if ( gNormLast < EPS * Math.max( 1.0, ArrayMath.norm( xLast ) ) ) {
						System.err.println( "Gradient is numerically zero, stopped on machine epsilon." );
						return eState.TERMINATE_GRADNORM;
					}
				}
			}

			System.err.println( " |" + nf.format( gNormLast ) + "| {" + nf.format( relNorm ) + "} " + nf.format( Math.abs( averageImprovement / newestVal ) ) + " " + ( evalsSize > 0 ? evals.get( evalsSize - 1 ).toString() : "-" ) + " " );
			return eState.CONTINUE;

		}

		public void add( double val, double[ ] grad, double[ ] x, int fevals, double evalScore ) {
			if ( !memoryConscious ) {
				if ( gNorms.size() > maxSize ) {
					gNorms.remove( 0 );
				}
				if ( time.size() > maxSize ) {
					time.remove( 0 );
				}
				if ( funcEvals.size() > maxSize ) {
					funcEvals.remove( 0 );
				}
				gNorms.add( gNormLast );
				time.add( howLong() );
				funcEvals.add( fevals );
			} else {
				maxSize = 10;
			}

			gNormLast = ArrayMath.norm( grad );
			if ( values.size() > maxSize ) {
				values.remove( 0 );
			}

			values.add( val );

			if ( evalScore != Double.NEGATIVE_INFINITY )
				evals.add( evalScore );

			System.err.print( nf.format( val ) + " " + nfsec.format( howLong() ) + "s" );

			xLast = x;
		}

		public double howLong() {
			return ( ( System.currentTimeMillis() - startTime ) ) / 1000.0;
		}

		public double[ ] getBest() {
			return xBest;
		}

		public void start( double val, double[ ] grad, double[ ] x ) {
			startTime = System.currentTimeMillis();
			gNormInit = ArrayMath.norm( grad );
			xLast = x;
		}

	}

	//TODO Exceptions
	public static class SurpriseConvergence extends Throwable{

		private static final long serialVersionUID = 4290178321643529559L;

		public SurpriseConvergence( String s ){
			super( s );
		}
	}

	private static class MaxEvaluationsExceeded extends Throwable{

		private static final long serialVersionUID = 8044806163343218660L;

		public MaxEvaluationsExceeded( String s ){
			super( s );
		}
	}

}