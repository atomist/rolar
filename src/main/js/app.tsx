import * as React from 'react';
import * as ReactDOM from 'react-dom';
import * as _ from 'lodash';

type AppState = {
    logs: Array<LogRow>;
}

class App extends React.Component<any, AppState> {

    constructor(props: any) {
        super(props);
        this.state = {
            logs: new Array<LogRow>()
        };
    }

    componentDidMount() {
        let auth = "";
        if (this.props.auth) {
            auth = `&auth=${this.props.auth}`
        }
        const url = '/api/reactive/logs/' + this.props.path + '?after=' + this.props.after + auth;
        const source = new EventSource(url);
        source.addEventListener("message", (logResultsEvent: MessageEvent) => {
            const logResults = JSON.parse(logResultsEvent.data);
            if (logResults.lastKey && logResults.lastKey.closed) {
                const specifiedPath = this.props.path.split("/");
                if(_.isEqual(specifiedPath, logResults.lastKey.path)) {
                    source.close();
                }
            }
            this.state.logs.push(...logResults.logs);
            this.setState({
                logs: this.state.logs
            });
        });
    }

    render() {
        return (
            <LogList logs={this.state.logs}/>
        )
    }
}

class LogRow {
    timestamp: string;
    level: string;
    message: string;
}

type LogListProps = {
    logs: Array<LogRow>;
}

class LogList extends React.Component<LogListProps, any> {
    constructor(props: LogListProps) {
        super(props);
    }

    render() {
        const logs = this.props.logs.map(log =>
            <Log key={log.timestamp + log.message} log={log}/>
        );
        return (
            <table>
                <tbody>
                    {logs}
                </tbody>
            </table>
        )
    }
}

type LogProps = {
    log: LogRow;
}

class Log extends React.Component<LogProps, any> {
    constructor(props: LogProps) {
        super(props);
    }

    render() {
        return (
            <tr>
                <td><pre>{this.props.log.timestamp}</pre></td>
                <td><pre>{this.props.log.level}</pre></td>
                <td><pre>{this.props.log.message}</pre></td>
            </tr>
        )
    }
}

const root = document.getElementById('react');
ReactDOM.render(<App {...(root.dataset)} />, root);
