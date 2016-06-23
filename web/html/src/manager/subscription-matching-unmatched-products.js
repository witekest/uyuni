"use strict";

const React = require("react");
const StatePersistedMixin = require("../components/util").StatePersistedMixin;
const UtilComponent = require("./subscription-matching-util");
const CsvLink = UtilComponent.CsvLink;
const SystemLabel = UtilComponent.SystemLabel;
const PopUp = require("../components/popup").PopUp;
const {Table, Column, SearchField, SimpleTableDataModel} = require("../components/tableng");

const UnmatchedProducts = React.createClass({
  mixins: [StatePersistedMixin],

  getInitialState: function() {
    return {
        selectedProductId: null,
        tableModel: new SimpleTableDataModel(this.buildData(this.props))
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.products != nextProps.products ||
        this.props.unmatchedProductIds != nextProps.unmatchedProductIds) {
        this.state.tableModel.mergeData(this.buildData(nextProps));
        this.forceUpdate();
    }
  },

  buildData: function(props) {
      const products = props.products;
      return props.unmatchedProductIds.map((pid) => {
          const productName = products[pid].productName;
          const systemCount = products[pid].unmatchedSystemCount;
          return {
                 id: pid,
                 productName: productName,
                 systemCount: systemCount
               };
       });
  },

  rowKey: function(rowData) {
    return rowData.id;
  },

  sortByName: function(a, b) {
    return a.productName.toLowerCase().localeCompare(b.productName.toLowerCase());
  },

  sortByCpuCount: function(a, b) {
    return a.systemCount - b.systemCount;
  },

  showPopUp: function(id) {
    this.setState({selectedProductId: id});
  },

  closePopUp: function() {
    this.setState({selectedProductId: null});
  },

  render: function() {
    var body;
    if (this.props.unmatchedProductIds.length > 0) {
      body = (
        <div>
          <Table
            dataModel={this.state.tableModel}
            rowKeyFn={this.rowKey}
            initialSort="name"
            >
            <Column
                columnKey="name"
                sortFn={this.sortByName}
                header={t("Product name")}
                cell={ (row) => row.productName }
                />
            <Column
                columnKey="cpuCount"
                sortFn={this.sortByCpuCount}
                header={t("Unmatched system count")}
                cell={ (row) => row.systemCount }
                />
            <Column
                cell={ (row) =>  <button
                                className="btn btn-default btn-cell"
                                onClick={() => {this.showPopUp(row.id);}}
                                data-toggle="modal"
                                data-target="#unmatchedProductsPopUp">
                                {t("Show system list")}
                              </button> }
                />
          </Table>

          <CsvLink name="unmatched_product_report.csv" />

          <UnmatchedSystemPopUp
            systems={this.props.systems}
            products={this.props.products}
            selectedProductId={this.state.selectedProductId}
            onClosePopUp={this.closePopUp}
          />
        </div>
      );
    }
    else {
      body = <p>{t("No unmatching products are found.")}</p>
    }

    return (
      <div className="row col-md-12">
        <h2>{t("Unmatched Products")}</h2>
        {body}
      </div>
    );
  }
});


const UnmatchedSystemPopUp = React.createClass({

  getInitialState: function() {
    return {
        tableModel: new SimpleTableDataModel(this.buildTableData(this.props))
    };
  },

  componentWillReceiveProps(nextProps) {
    if (this.props.selectedProductId != nextProps.selectedProductId) {
        this.setState({tableModel: new SimpleTableDataModel(this.buildTableData(nextProps))});
    }
    else if (this.props.products != nextProps.products ||
            this.props.systems != nextProps.systems) {
        this.state.tableModel.mergeData(this.buildTableData(nextProps));
        this.forceUpdate();
    }
  },

  buildTableData: function(props) {
    if (!props.selectedProductId) {
        return [];
    }
    const product = props.products[props.selectedProductId];
    const systems = props.systems;
    return product.unmatchedSystemIds.map((sid) => {
      return {
        id: sid,
        systemName: systems[sid].name,
        type: systems[sid].type
      }
    });
  },

  sortByName: function(a, b) {
    return a.systemName.toLowerCase().localeCompare(b.systemName.toLowerCase());
  },

  searchData: function(data, criteria) {
    return data.filter((row) => row.systemName.toLowerCase().indexOf(criteria.toLowerCase()) > -1);
  },

  rowKey: function(rowData) {
    return rowData.id;
  },

  render: function() {
    const popUpContent = <Table
        dataModel={this.state.tableModel}
        rowKeyFn={this.rowKey}
        initialSort="name"
        searchPanel={
            <SearchField searchFn={this.searchData} placeholder="Filter by name"/>
        }>
        <Column
            columnKey="name"
            sortFn={this.sortByName}
            header={t("System name")}
            cell={ (row) => <SystemLabel type={row.type} name={row.systemName} /> } />
      </Table>;

    return (
      <PopUp title={t("Unmatched systems")}
        id="unmatchedProductsPopUp"
        content={popUpContent}
        onClosePopUp={this.props.onClosePopUp}
      />
    );
  }
});

module.exports = {
  UnmatchedProducts: UnmatchedProducts,
}
