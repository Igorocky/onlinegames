"use strict";

function degToRad(deg) {
    return deg/180*Math.PI
}

class Point {
    constructor(x,y) {
        this.x = x
        this.y = y
    }

    /**
     * Rotates length of a vector which starts at (0,0) and ends at this point.
     * @returns {number}
     */
    // length() {
    //     return Math.sqrt(this.x**2 + this.y**2)
    // }

    /**
     * @param {Point} otherPoint
     * @returns {Point}
     */
    plus(otherPoint) {
        return new Point(this.x+otherPoint.x, this.y+otherPoint.y)
    }

    /**
     * @param {Point} otherPoint
     * @returns {Point}
     */
    minus(otherPoint) {
        return new Point(this.x-otherPoint.x, this.y-otherPoint.y)
    }

    /**
     * @param {number} factor
     * @returns {Point}
     */
    scale(factor) {
        return new Point(this.x*factor, this.y*factor)
    }

    /**
     * Returns a new point obtained by moving this point in 'dir' direction 'dist' times.
     * @param {Point} dir
     * @param {number} dist
     * @returns {Point}
     */
    move(dir, dist) {
        dist = dist??1
        return new Point(this.x + dir.x*dist, this.y + dir.y*dist)
    }

    /**
     * Rotates a vector which starts at (0,0) and ends at this point by 'deg' degree and returns end point of the resulting vector.
     * @param {number} deg
     * @returns {Point}
     */
    rotate(deg) {
        const rad = -degToRad(deg)
        return new Point(
            this.x*Math.cos(rad) - this.y*Math.sin(rad),
            this.x*Math.sin(rad) + this.y*Math.cos(rad)
        )
    }
}

class Vector {
    /**
     * @param {Point} start
     * @param {Point} end
     */
    constructor(start, end) {
        this.start = start
        this.end = end
    }

    /**
     * Returns a new vector obtained by rotating this vector around its start by 'deg' degree.
     * @param {number} deg
     * @returns {Vector}
     */
    rotate(deg) {
        //todo: optimize performance
        return new Vector(
            this.start,
            this.start.plus(
                this.end.minus(this.start).rotate(deg)
            )
        )
    }

    /**
     * @returns {Vector}
     */
    normalize() {
        //todo: optimize performance
        const local = this.end.minus(this.start)
        const len = local.length()
        const normalizedLocal = local.scale(1/len)
        return new Vector(
            this.start,
            this.start.plus(normalizedLocal)
        )
    }

    /**
     * @param {Vector} vec
     * @returns {Vector}
     */
    translate(vec) {
        //todo: optimize performance
        const delta = this.end.minus(this.start)
        return new Vector(
            this.start.plus(delta),
            this.end.plus(delta)
        )
    }

    /**
     *
     * @param {number} factor
     * @returns {Vector}
     */
    scale(factor) {
        //todo: optimize performance
        return new Vector(
            this.start,
            this.start.plus(
                this.end.minus(this.start).scale(factor)
            )
        )
    }

    toSvgLine(props) {
        return svgLine({from:this.start, to:this.end, props})
    }
}

const SVG_EX = new Vector(new Point(0,0), new Point(1,0))
const SVG_EY = new Vector(new Point(0,0), new Point(0,-1))

/**
 * Returns SVG line object.
 * @param {Point} from
 * @param {Point} to
 * @param {Object} props
 */
function svgLine({from, to, props}) {
    return SVG.line({x1:from.x, y1:from.y, x2:to.x, y2:to.y, ...(props??{})})
}

//tests
function assertTrue(bool) {
    if (!bool) {
        throw new Error(`Assertion failed: expected true but was false.`)
    }
}

function assertEquals(expected, actual) {
    if (expected !== actual) {
        throw new Error(`Assertion failed: expected ${JSON.stringify(expected)} but was ${JSON.stringify(actual)}.`)
    }
}

function assertNumbersEqual(expected, actual, precision) {
    if (Math.abs(expected - actual) > precision) {
        throw new Error(`Assertion failed: expected ${JSON.stringify(expected)} but was ${JSON.stringify(actual)}.`)
    }
}

const TESTS = [
    'testPointMove',
    'testVectorRotate',
]

function testPointMove() {
    const origin = new Point(0,0)
    const ex = new Point(1,0)
    const ey = new Point(0,-1)

    const z = origin.move(ex)
    assertEquals(1, z.x)
    assertEquals(0, z.y)

    const a = origin.move(ex, 5)
    assertEquals(5, a.x)
    assertEquals(0, a.y)

    const b = origin.move(ey, 4)
    assertEquals(0, b.x)
    assertEquals(-4, b.y)

    const precision = 0.0000001
    const distC = 7
    const c = origin.move(ex.rotate(45), distC)
    assertTrue(c.x > 0)
    assertTrue(c.y < 0)
    assertNumbersEqual(distC, (c.x**2+c.y**2)**0.5, precision)
}

function testVectorRotate() {
    const precision = 0.0000001
    const v1 = new Vector(new Point(0,0), new Point(5,0))
    const v1R90 = v1.rotate(90)
    assertNumbersEqual(0, v1R90.x, precision)
    assertNumbersEqual(-5, v1R90.y, precision)
}

TESTS.forEach(test => window[test]())