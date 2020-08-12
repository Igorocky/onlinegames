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
     * @param {number} stepSize
     * @param {number} numOfLines
     * @param {number} height
     * @param {Object} props
     * @return {SvgElements} svg elements
     */
    function renderVerticalGrid({key, ex, stepSize, numOfLines, height, props}) {
        const ey = ex.rotate(90)
        const lineVectors = ints(0, numOfLines-1)
            .map(lineNum => ex.scale(lineNum*stepSize))
            .map(eyTranslation => ey.translate(eyTranslation))
            .map(eyi => eyi.scale(height))
        const lines = lineVectors
            .map((lineVector,idx) => lineVector.toSvgLine({key: `${key}-${idx}`, ...(props?props:{})}))

        return {
            svgElems: lines,
            boundaries: mergeSvgBoundaries(lineVectors.map(v=>v.boundaries()))
        }
    }

    /**
     * @return {SvgElements} SvgElements
     */
    function renderGrid({key, ex, stepSize, colNum, rowNum, props}) {
        const stepAbsoluteSize = stepSize*ex.length()
        const verticalGrid = renderVerticalGrid({
            key: key + '-verticalGrid',
            ex: ex,
            numOfLines: colNum+1,
            stepSize,
            height: rowNum*stepAbsoluteSize,
            props
        })
        const horizontalGrid = renderVerticalGrid({
            key: key + '-horizontalGrid',
            ex: ex.rotate(90).translate(ex.scale(colNum*stepAbsoluteSize)),
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
        const background = SVG.rect({key:'background', x:-1000, y:-1000, width:2000, height:2000, fill:"lightgrey"})

        const grid = renderGrid({
            key: 'XOGrid',
            ex: SVG_EX,
            stepSize: 10,
            colNum: 3,
            rowNum: 3,
            props: {stroke:'green', strokeWidth: 0.1}
        })

        return RE.svg({width:300, height:300, boundaries: grid.boundaries.addAbsoluteMargin(3)},
            background,
            ...grid.svgElems
        )
    }

    return renderSvgField()
}