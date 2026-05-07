// -----------------------------------------------------------
//
// Rules
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import ControllerPage from "../component/ControllerPage";
import {Select} from "carbon-components-react";
import RestCallService from "../services/RestCallService";

class Rules extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            status: "",
            display: {
                loading: false
            },
            rules: [],
            isOpen: false
        };
    }

    componentDidMount() {
    }

    /*           {JSON.stringify(this.state.runners, null, 2) } */
    render() {
        return (
            <div className="container">
                <h1 className="title">Rules</h1>

                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <ControllerPage errorMessage={this.state.status} loading={this.state.display.loading}/>
                    </div>
                </div>
                <div className="row" style={{width: "100%"}}>>
                    <Select
                        value={this.state.display.version}
                        labelText="Version"
                        disabled={this.state.display.loading}
                        onChange={(event) => this.setVersion(event.target.value)}>
                        <option value="87-to-88">8.7 to 8.8</option>
                    </Select>
                    <button>Load rules</button>
                </div>
                <div className="row" style={{width: "100%"}}>>
                    <table id="rulesTable" className="table is-hoverable is-fullwidth">
                        <thead>
                        <tr>
                            <th>Runner Name</th>
                            <th>Type</th>
                            <th>Type Runner</th>
                            <th>Class Name</th>
                        </tr>
                        </thead>
                        <tbody>
                        {this.state.rules ? this.state.rules.map((runner, _index) =>
                            <tr onClick={() => this.openModal(runner)}>
                                <td>
                                    <img style={{width: "30px"}} src={runner.logo} alt="logo runner"/>
                                    &nbsp;
                                    {runner.name}</td>
                                <td>{runner.type}</td>
                                <td>{runner.typeRunner}</td>
                                <td>{runner.className}</td>
                            </tr>
                        ) : <tr/>}
                        </tbody>
                    </table>
                </div>
            </div>
        )

    }


    refreshList() {
        console.log("Rules.refreshList http[upgrated/api/runner/list]");
        this.setState({runners: [], status: ""});
        var restCallService = RestCallService.getInstance();
        restCallService.getJson('upgrated/api/runner/list?', this, this.refreshListCallback);
    }

    refreshListCallback(httpPayload) {
        if (httpPayload.isError()) {
            this.setState({status: httpPayload.getError()});
        } else {
            this.setState({runners: httpPayload.getData()});

        }
    }


}

export default Rules;
