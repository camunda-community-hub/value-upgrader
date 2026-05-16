// -----------------------------------------------------------
//
// Rules
//
// List of all runners available
//
// -----------------------------------------------------------

import React from 'react';
import ControllerPage from "../component/ControllerPage";
import {Button, CodeSnippet, Select} from "carbon-components-react";
import RestCallService from "../services/RestCallService";

class ReviewNonDefault extends React.Component {


    constructor(_props) {
        super();
        this.state = {
            status: "",
            display: {
                version: "V87_88",
                loading: false
            },
            result: "",
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
                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-6">
                        <Select
                            value={this.state.display.version}
                            labelText="Version"
                            disabled={this.state.display.loading}
                            onChange={(event) => this.setVersion(event.target.value)}>
                            <option value="V87_88">8.7 to 8.8</option>
                            <option value="V88_89">8.8 to 8.9</option>
                        </Select>
                    </div>
                </div>

                <div className="row" style={{width: "100%", paddingTop: "10px"}}>
                    <div className="col-md-6">
                        <Button onClick={() => this.loadRule()}
                                disabled={this.state.display.loading}>Get rule</Button>
                    </div>
                </div>


                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <h2>Rule</h2>
                        <span style={{
                            fontWeight: "bold",
                            fontSize: "0.8em",
                            margin: "5px 5px 5px 5px",
                            display: "block"
                        }}>
                        </span>
                    </div>
                </div>


                <div className="row" style={{width: "100%"}}>
                    <div className="col-md-12">
                        <CodeSnippet
                            type="multi"
                            feedback="Copied!"
                            wrapText>
                            {this.state.result}
                        </CodeSnippet>
                    </div>
                </div>
            </div>
        )

    }

    setVersion(value) {
        // console.log("DashBoard.setOrderBy: " + value);
        this.setDisplayProperty("version", value);
    }

    loadRule(event) {
        console.log("loadRule version [" + this.state.display.version);
        let url = '/rule/api/v1/content?version=' + this.state.display.version;
        console.log("URL: " + url);

        let restCallService = RestCallService.getInstance();

        /* formData.append("File", this.state.files[0]); */
        this.setDisplayProperty("loading", true);

        restCallService.getJson(url, this, this.loadRuleCallback);


        // dispatch(connectorService.uploadJar(event.target.files[0]));
    }

    loadRuleCallback(httpResponse) {
        console.log("loadRuleCallback start");
        this.setDisplayProperty("loading", false);

        if (httpResponse.isError()) {
            console.log("Rules.loadRuleCallback: error " + httpResponse.getError());
            this.setState({status: httpResponse.getError()});
        } else {
            this.setState({status:"", "result": httpResponse.getData()})
        }
    }

    /**
     * Set the display property
     * @param propertyName name of the property
     * @param propertyValue the value
     */
    setDisplayProperty(propertyName, propertyValue) {
        let displayObject = this.state.display;
        displayObject[propertyName] = propertyValue;
        this.setState({display: displayObject});
    }

}

export default ReviewNonDefault;
