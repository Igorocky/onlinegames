"use strict";

const XoGamePlayfieldComponent = ({tableData, onCellClicked}) => {

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

    /**
     * @param {Vector} centerEx
     * @param {number} cellSize
     */
    function renderSymbol({centerEx, symbol, cellSize}) {
        const key = `cell-symbol-${centerEx.start.x}-${centerEx.start.y}`
        const cellAbsoluteSize = centerEx.length() * cellSize
        if (symbol === 'x') {
            return [
                centerEx.rotate(45),
                centerEx.rotate(135),
                centerEx.rotate(-45),
                centerEx.rotate(-135)
            ]
                .map(vec => vec.scale(cellSize*0.35))
                .map((vec, idx) => vec.toSvgLine({
                    key: key + '-' + idx, stroke: 'blue', strokeWidth: cellAbsoluteSize * 0.15, strokeLinecap: 'round'
                }))
        } else if (symbol === 'o') {
            return [
                svgCircle({
                    key,
                    c: centerEx.start,
                    r: cellAbsoluteSize * 0.3,
                    props: {fill: 'transparent', stroke: 'orange', strokeWidth: cellAbsoluteSize * 0.15}
                })
            ]
        }
    }

    function renderCell({ex, cellDto, cellSize}) {
        if (cellDto) {
            return [
                ...renderSymbol({
                    symbol: cellDto.symbol,
                    cellSize,
                    centerEx: ex.translate(null, cellSize / 2).translate(ex.rotate(90), cellSize / 2),
                }),
            ]
        } else {
            return []
        }
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

    function renderSvgField() {
        const playFieldSize = 300
        const background = SVG.rect({key:'background', x:-1000, y:-1000, width:2000, height:2000, fill:"lightgrey"})

        const cellSize = 10
        const ex = SVG_EX
        const grid = renderGrid({
            key: 'XOGrid',
            ex,
            stepSize: cellSize,
            colNum: 3,
            rowNum: 3,
            props: {stroke:'green', strokeWidth: 0.1}
        })

        return RE.svg({width: playFieldSize, height: playFieldSize, boundaries: grid.boundaries.addAbsoluteMargin(3)},
            background,
            ...grid.svgElems,
            ...renderCells({ex, cellSize, tableData})
        )
    }

    return renderSvgField()
}