"use strict";

const XoGamePlayfieldComponent = ({size, fieldSize, tableData, onCellClicked, frameSymbol}) => {

    /**
     * @typedef {Object} SvgElements
     * @property {Object[]} svgElems
     * @property {SvgBoundaries} boundaries
     */

    /**
     * Note: visual step size is ex.length()*stepSize
     * @param {string} key
     * @param {Vector} ex
     * @param {Vector} ey
     * @param {number} stepSize
     * @param {number} numOfLines
     * @param {number} height
     * @param {Object} props
     * @return {SvgElements} svg elements
     */
    function renderVerticalGrid({key, ex, ey, stepSize, numOfLines, height, props}) {
        props = props??{}
        const lineVectors = ints(0, numOfLines-1)
            .map(lineNum => ex.translate(null,lineNum*stepSize))
            .map(exi => ey.translateTo(exi.start))
            .map(eyi => eyi.scale(height))
        const lines = lineVectors
            .map((lineVector,idx) => lineVector.toSvgLine({key: `${key}-${idx}`, ...props}))

        return {
            svgElems: lines,
            boundaries: mergeSvgBoundaries(lineVectors.map(v=>v.boundaries()))
        }
    }

    function getColorForSymbol(symbol) {
        if (symbol === 'x') {
            return 'dodgerblue'
        } else if (symbol === 'o') {
            return 'sandybrown'
        } else if (symbol === 's') {
            return 'lightcoral'
        } else if (symbol === 't') {
            return 'mediumaquamarine'
        } else if (symbol === 'a') {
            return 'orchid'
        }
    }

    /**
     * @param {Vector} centerEx
     * @param {number} cellSize
     */
    function renderSymbol({centerEx, symbol, cellSize}) {
        const key = `cell-symbol-${centerEx.start.x}-${centerEx.start.y}`
        const cellAbsoluteSize = centerEx.length() * cellSize
        const strokeWidth = cellAbsoluteSize * 0.15
        const symbolColor = getColorForSymbol(symbol)
        if (symbol === 'x') {
            const xStrokeWidth = cellAbsoluteSize * 0.30
            return [
                centerEx.rotate(45),
                centerEx.rotate(135),
                centerEx.rotate(-45),
                centerEx.rotate(-135)
            ]
                .map(vec => vec.scale(cellSize*0.25))
                .map((vec, idx) => vec.toSvgLine({
                    key: key + '-' + idx, stroke: symbolColor, strokeWidth:xStrokeWidth, strokeLinecap: 'round'
                }))
        } else if (symbol === 'o') {
            return [
                svgCircle({
                    key,
                    c: centerEx.start,
                    r: cellAbsoluteSize * 0.3,
                    props: {fill: symbolColor, stroke: symbolColor, strokeWidth}
                })
            ]
        } else if (symbol === 's') {
            const baseVector = centerEx.rotate(45).scale(cellSize*0.37)
            return [
                svgPolygon({
                    key,
                    points: [baseVector.end, baseVector.rotate(90).end, baseVector.rotate(180).end, baseVector.rotate(270).end],
                    props: {fill: symbolColor, stroke: symbolColor, strokeWidth, strokeLinejoin: 'round'}
                })
            ]
        } else if (symbol === 't') {
            const baseVector = centerEx.rotate(90).translate(null, -cellSize*0.07).scale(cellSize*0.34)
            return [
                svgPolygon({
                    key,
                    points: [baseVector.end, baseVector.rotate(120).end, baseVector.rotate(-120).end],
                    props: {fill: symbolColor, stroke: symbolColor, strokeWidth, strokeLinejoin: 'round'}
                })
            ]
        } else if (symbol === 'a') {
            const baseVector1 = centerEx.rotate(90).scale(cellSize*0.3)
            const baseVector2 = centerEx.rotate(90+36).scale(cellSize*0.15)
            return [
                svgPolygon({
                    key,
                    points: [
                        baseVector1.end,
                        baseVector2.end,
                        baseVector1.rotate(72*1).end,
                        baseVector2.rotate(72*1).end,
                        baseVector1.rotate(72*2).end,
                        baseVector2.rotate(72*2).end,
                        baseVector1.rotate(72*3).end,
                        baseVector2.rotate(72*3).end,
                        baseVector1.rotate(72*4).end,
                        baseVector2.rotate(72*4).end,
                    ],
                    props: {fill: symbolColor, stroke: symbolColor, strokeWidth, strokeLinejoin: 'round'}
                })
            ]
        }
    }

    function renderCell({ex, cellDto, cellSize}) {
        const cellAbsoluteSize = ex.length()*cellSize
        const result = []
        if (cellDto.isWinnerCell) {
            result.push(
                SVG.rect({
                    key: 'cell-winner-' + ex.start.x + '-' + ex.start.y,
                    x: ex.start.x,
                    y: ex.start.y-cellAbsoluteSize,
                    width: cellAbsoluteSize,
                    height: cellAbsoluteSize,
                    stroke: 'none',
                    strokeWidth: 1,
                    fill: 'lightyellow'
                })
            )
        }
        if (cellDto.lastCell) {
            result.push(
                SVG.rect({
                    key: 'cell-last-' + ex.start.x + '-' + ex.start.y,
                    x: ex.start.x,
                    y: ex.start.y-cellAbsoluteSize,
                    width: cellAbsoluteSize,
                    height: cellAbsoluteSize,
                    stroke: 'none',
                    strokeWidth: 1,
                    fill: 'mistyrose'
                })
            )
        }
        if (cellDto.symbol) {
            result.push(
                ...renderSymbol({
                    symbol: cellDto.symbol,
                    cellSize,
                    centerEx: ex.translate(null, cellSize / 2).translate(ex.rotate(90), cellSize / 2),
                })
            )
        }
        result.push(
            SVG.rect({
                key: 'cell-click-pane-' + ex.start.x + '-' + ex.start.y,
                x: ex.start.x,
                y: ex.start.y-cellAbsoluteSize,
                width: cellAbsoluteSize,
                height: cellAbsoluteSize,
                stroke: 'none',
                strokeWidth: 1,
                fill: 'transparent',
                onClick: () => onCellClicked({x:cellDto.x, y:cellDto.y})
            })
        )
        return result
    }

    function renderCells({ex, cellSize, tableData}) {
        const cells = []
        const ey = ex.rotate(90)
        for (let x = 0; x < tableData.length; x++) {
            const xShift = x*cellSize
            for (let y = 0; y < tableData[x].length; y++) {
                cells.push(...renderCell({
                    ex: ex.translate(null, xShift).translate(ey, y*cellSize),
                    cellSize,
                    cellDto: tableData[x][y]
                }))
            }
        }
        return cells
    }

    /**
     * @return {SvgElements} SvgElements
     */
    function renderGrid({key, ex, stepSize, colNum, rowNum, props}) {
        const stepAbsoluteSize = stepSize*ex.length()
        const ey = ex.rotate(90);
        const verticalGrid = renderVerticalGrid({
            key: key + '-verticalGrid',
            ex,
            ey,
            numOfLines: colNum+1,
            stepSize,
            height: rowNum*stepAbsoluteSize,
            props
        })
        const ex2 = ey.translate(ex, colNum*stepSize);
        const horizontalGrid = renderVerticalGrid({
            key: key + '-horizontalGrid',
            ex: ex2,
            ey: ex2.rotate(90),
            numOfLines: rowNum+1,
            stepSize,
            height: colNum*stepAbsoluteSize,
            props
        })
        return {
            svgElems: [...verticalGrid.svgElems, ...horizontalGrid.svgElems],
            boundaries: mergeSvgBoundaries([verticalGrid.boundaries, horizontalGrid.boundaries])
        }
    }

    function renderLineOfFrame({ex, margin, symbol, cellSize, amount}) {
        let ex2 = ex.translate(ex.rotate(180), margin/2).scale(margin/cellSize*1.2)
        const shift = ex.rotate(90).scale(cellSize)

        const result = []
        while (amount > 0) {
            result.push(
                ...renderSymbol({centerEx: ex2, symbol, cellSize})
            )
            ex2 = ex2.translate(shift)
            amount = amount - 1
        }

        return result
    }

    function renderLine2OfFrame({ex, margin, symbol, cellSize, fieldSize}) {
        const exNeg = ex.rotate(180).scale(margin);
        const v1 = ex
        const v2 = ex.rotate(90).scale(cellSize * fieldSize)
        const v3 = v2.add(exNeg)
        const v4 = exNeg
        const points = [v1.start, v2.end, v3.end, v4.end];
        return [
            svgPolygon({
                key: 'frame-polygon1-' + ex.start.x + "-" + ex.start.y,
                points,
                props: {fill: getColorForSymbol(symbol), strokeWidth: 0, opacity: 0.6}
            })
        ]
    }

    function renderFrame({ex, boundaries, margin, symbol, cellSize, fieldSize}) {
        const amount = 1
        const baseExArr = [
            ex.translateTo(new Point(boundaries.minX, boundaries.maxY)),
            ex.rotate(90).translateTo(new Point(boundaries.maxX, boundaries.maxY)),
            ex.rotate(180).translateTo(new Point(boundaries.maxX, boundaries.minY)),
            ex.rotate(270).translateTo(new Point(boundaries.minX, boundaries.minY))
        ]
        return [
            ...baseExArr
                .map(ex => ex.translate(ex.rotate(-90), margin / 2.2))
                .flatMap(ex => renderLineOfFrame({ex, margin, symbol, cellSize, amount})),
            ...baseExArr
                .flatMap(ex => renderLine2OfFrame({ex, margin, symbol, cellSize, fieldSize})),

        ]
    }

    function renderSvgField() {
        const background = SVG.rect({key:'background', x:-1000, y:-1000, width:2000, height:2000, fill:"lightgrey"})

        const cellSize = 10
        const ex = SVG_EX
        const grid = renderGrid({
            key: 'XOGrid',
            ex,
            stepSize: cellSize,
            colNum: fieldSize,
            rowNum: fieldSize,
            props: {stroke:'white', strokeWidth: 0.2}
        })

        const margin = cellSize*0.3

        return RE.svg({width: size, height: size, boundaries: grid.boundaries.addAbsoluteMargin(margin)},
            background,
            ...grid.svgElems,
            ...renderCells({ex, cellSize, tableData}),
            frameSymbol?renderFrame({ex, boundaries: grid.boundaries, margin, symbol:frameSymbol, cellSize, fieldSize}):null,
        )
    }

    return renderSvgField()
}