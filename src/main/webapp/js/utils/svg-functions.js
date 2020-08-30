"use strict";

class SvgBoundaries {
    constructor(minX, maxX, minY, maxY) {
        this.minX = minX
        this.maxX = maxX
        this.minY = minY
        this.maxY = maxY
    }

    addAbsoluteMargin(margin) {
        return new SvgBoundaries(
            this.minX - margin,
            this.maxX + margin,
            this.minY - margin,
            this.maxY + margin,
        )
    }
}

class Point {
    constructor(x,y) {
        this.x = x
        this.y = y
    }

    /**
     * Returns length of a vector which starts at (0,0) and ends at this point.
     * @returns {number}
     */
    length() {
        return Math.sqrt(this.x**2 + this.y**2)
    }

    /**
     * @param {Point} otherPoint
     * @returns {Point}
     */
    add(otherPoint) {
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

    length() {
        return this.end.minus(this.start).length()
    }

    /**
     * Returns a new vector obtained by rotating this vector around its start by 'deg' degree.
     * @param {number} deg
     * @returns {Vector}
     */
    rotate(deg) {
        return new Vector(
            this.start,
            this.start.add(
                this.end.minus(this.start).rotate(deg)
            )
        )
    }

    /**
     * @returns {Vector}
     */
    normalize() {
        const local = this.end.minus(this.start)
        const len = local.length()
        const normalizedLocal = local.scale(1/len)
        return new Vector(
            this.start,
            this.start.add(normalizedLocal)
        )
    }

    /**
     * @param {Vector} [vec]
     * @param {number} [dist]
     * @returns {Vector}
     */
    translate(vec, dist) {
        vec = vec??this
        let delta = vec.end.minus(vec.start)
        if (hasValue(dist)) {
            delta = delta.scale(dist)
        }
        return new Vector(
            this.start.add(delta),
            this.end.add(delta)
        )
    }

    translateTo(point) {
        return this.translate(new Vector(this.start, point))
    }

    /**
     *
     * @param {number} factor
     * @returns {Vector}
     */
    scale(factor) {
        return new Vector(
            this.start,
            this.start.add(
                this.end.minus(this.start).scale(factor)
            )
        )
    }

    add(anotherVector) {
        return new Vector(
            this.start,
            this.end.add(
                anotherVector.end.minus(anotherVector.start)
            )
        )
    }

    /**
     * @return {SvgBoundaries} SvgBoundaries
     */
    boundaries() {
        return new SvgBoundaries(
            Math.min(this.start.x, this.end.x),
            Math.max(this.start.x, this.end.x),
            Math.min(this.start.y, this.end.y),
            Math.max(this.start.y, this.end.y),
        )
    }

    toSvgLine(props) {
        return svgLine({from:this.start, to:this.end, props})
    }
}

const SVG_EX = new Vector(new Point(0,0), new Point(1,0))
const SVG_EY = new Vector(new Point(0,0), new Point(0,-1))

function degToRad(deg) {
    return deg/180*Math.PI
}

/**
 * @param {SvgBoundaries[]} boundariesList
 */
function mergeSvgBoundaries(boundariesList) {
    return boundariesList.reduce((prev, curr) => new SvgBoundaries(
            Math.min(prev.minX, curr.minX),
            Math.max(prev.maxX, curr.maxX),
            Math.min(prev.minY, curr.minY),
            Math.max(prev.maxY, curr.maxY),
    ))
}

/**
 * Returns SVG line object.
 * @param {Point} from
 * @param {Point} to
 * @param {Object} props
 */
function svgLine({from, to, props}) {
    return SVG.line({x1:from.x, y1:from.y, x2:to.x, y2:to.y, ...(props??{})})
}

/**
 * @param {string} key
 * @param {Point} c
 * @param {number} r
 * @param {Object} props
 */
function svgCircle({key, c, r, props}) {
    props = props??{}
    return SVG.circle({key, cx:c.x, cy:c.y, r, ...props})
}

/**
 * @param {string} key
 * @param {Point[]} points
 * @param {Object} props
 * @return {*}
 */
function svgPolygon({key, points, props}) {
    props = props??{}
    return SVG.polygon({key, points: points.flatMap(point => [point.x, point.y]).join(' '), ...props})
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
    precision = precision??0.0000000001
    if (Math.abs(expected - actual) > precision) {
        throw new Error(`Assertion failed: expected ${JSON.stringify(expected)} but was ${JSON.stringify(actual)}.`)
    }
}

const TESTS = [
    'testPointMove',
    'testVectorRotate',
    'testVectorScale',
    'testVectorTranslate',
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

    const distC = 7
    const c = origin.move(ex.rotate(45), distC)
    assertTrue(c.x > 0)
    assertTrue(c.y < 0)
    assertNumbersEqual(distC, (c.x**2+c.y**2)**0.5)
}

function testVectorRotate() {
    const v1 = new Vector(new Point(0,0), new Point(5,0))
    const v1R90 = v1.rotate(90)
    assertNumbersEqual(0, v1R90.x)
    assertNumbersEqual(-5, v1R90.y)
}

function testVectorScale() {
    const v1 = new Vector(new Point(1,2), new Point(3,4))
    const v1Scaled = v1.scale(5)
    assertNumbersEqual(1, v1Scaled.start.x)
    assertNumbersEqual(2, v1Scaled.start.y)
    assertNumbersEqual(11, v1Scaled.end.x)
    assertNumbersEqual(12, v1Scaled.end.y)
}

function testVectorTranslate() {
    const v1 = new Vector(new Point(1,2), new Point(3,4))
    const v1Translated = v1.translate(SVG_EX)
    assertNumbersEqual(2, v1Translated.start.x)
    assertNumbersEqual(2, v1Translated.start.y)
    assertNumbersEqual(4, v1Translated.end.x)
    assertNumbersEqual(4, v1Translated.end.y)

    const v2 = SVG_EX
    const v2Translated = v2.translate(null,0)
    assertNumbersEqual(0, v2Translated.start.x)
    assertNumbersEqual(0, v2Translated.start.y)
    assertNumbersEqual(1, v2Translated.end.x)
    assertNumbersEqual(0, v2Translated.end.y)
}

TESTS.forEach(test => window[test]())